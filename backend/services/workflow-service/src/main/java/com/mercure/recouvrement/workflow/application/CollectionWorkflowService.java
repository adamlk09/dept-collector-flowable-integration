package com.mercure.recouvrement.workflow.application;

import com.deptcollector.shared.tenant.TenantContext;
import com.mercure.recouvrement.workflow.audit.OutboxEventWriter;
import com.mercure.recouvrement.workflow.client.NotificationClient;
import com.mercure.recouvrement.workflow.dto.IncidentDto;
import com.mercure.recouvrement.workflow.dto.JobDto;
import com.mercure.recouvrement.workflow.dto.ProcessHistoryDto;
import com.mercure.recouvrement.workflow.dto.StartCollectionProcessRequest;
import com.mercure.recouvrement.workflow.dto.StartCollectionProcessResponse;
import com.mercure.recouvrement.workflow.dto.StartProcessResponse;
import com.mercure.recouvrement.workflow.dto.TaskDto;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CollectionWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(CollectionWorkflowService.class);
    private static final String PROCESS_KEY = "debtCollectionProcess";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final ManagementService managementService;
    private final NotificationClient notificationClient;
    private final OutboxEventWriter outboxEventWriter;

    public CollectionWorkflowService(RuntimeService runtimeService,
                                     TaskService taskService,
                                     HistoryService historyService,
                                     ManagementService managementService,
                                     NotificationClient notificationClient,
                                     OutboxEventWriter outboxEventWriter) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
        this.managementService = managementService;
        this.notificationClient = notificationClient;
        this.outboxEventWriter = outboxEventWriter;
    }

    /**
     * Synchronous debt-collection entry point. Starts the BPMN process, which now runs in the
     * calling thread only up to its one wait state — the {@code reviewQualification} user task —
     * then returns. {@code qualification}/{@code qualificationReason}/{@code segment} are already
     * set by segmentDebt by that point; {@code strategy}/{@code status} are null until an agent
     * completes the task and the process runs on through the gateway to a call activity or end
     * event. Decision logic lives entirely in the BPMN delegates and gateway — never here.
     */
    public StartCollectionProcessResponse startCollectionProcess(StartCollectionProcessRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();

        Map<String, Object> vars = buildVariables(request, tenantId);

        // The BPMN is auto-deployed to the default (tenantless) deployment, so the process
        // is started without a tenant; the tenant is retained as a process variable for traceability.
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                PROCESS_KEY, request.debtId(), vars);

        String pid = instance.getId();
        log.info("Collection process reached review task pid={} debtId={} tenantId={}", pid, request.debtId(), tenantId);

        String qualification = (String) requireHistoricVariable(pid, "qualification");
        String qualificationReason = (String) requireHistoricVariable(pid, "qualificationReason");
        String segment = (String) requireHistoricVariable(pid, "segment");
        String strategy = (String) optionalHistoricVariable(pid, "strategy");
        String status = (String) optionalHistoricVariable(pid, "status");

        return new StartCollectionProcessResponse(
                pid, request.debtId(), request.customerId(),
                qualification, qualificationReason, segment, strategy, status);
    }

    /**
     * Builds the behavioral process variables (EPIC 1–6 categories) from the request, applying
     * a safe categorical default for every dimension the caller left unset.
     */
    private Map<String, Object> buildVariables(StartCollectionProcessRequest request, String tenantId) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("debtId", request.debtId());
        vars.put("customerId", request.customerId());
        vars.put("clientInfoStatus", orDefault(request.clientInfoStatus(), "CLIENT_INFORMATION_COMPLETE"));
        vars.put("contractStatus", orDefault(request.contractStatus(), "ACTIVE_CONTRACT"));
        vars.put("debtStatus", orDefault(request.debtStatus(), "OPEN_DEBT"));
        vars.put("paymentHistory", orDefault(request.paymentHistory(), "GOOD_PAYMENT_HISTORY"));
        vars.put("collectionStage", orDefault(request.collectionStage(), "FIRST_CONTACT"));
        vars.put("promiseStatus", orDefault(request.promiseStatus(), "NONE"));
        vars.put("reachable", request.reachable() == null ? Boolean.TRUE : request.reachable());
        vars.put("tenantId", tenantId);
        return vars;
    }

    private static String orDefault(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private Object requireHistoricVariable(String processInstanceId, String name) {
        HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName(name)
                .singleResult();
        if (variable == null || variable.getValue() == null) {
            throw new IllegalStateException("Historic variable '" + name
                    + "' was not produced by process instance " + processInstanceId
                    + " — the workflow did not complete as expected");
        }
        return variable.getValue();
    }

    private Object optionalHistoricVariable(String processInstanceId, String name) {
        HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName(name)
                .singleResult();
        return variable != null ? variable.getValue() : null;
    }

    public StartProcessResponse startProcess(StartCollectionProcessRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();

        // The BPMN auto-deploys to the default (tenantless) deployment (see
        // startCollectionProcess above), so instances are never tagged with a native Flowable
        // tenant either — tagging one here would make Flowable resolve the process definition
        // "for tenant X", which doesn't exist, and throw. Tenant stays a process variable only.
        //
        // The current BPMN also has no wait states, so a started instance may already have run
        // to completion (and left the runtime tables) by the time a retry arrives. Dedup against
        // history — which holds both active and completed instances — not runtimeService, or
        // every retry of an already-finished process would start a duplicate.
        HistoricProcessInstance existing = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(PROCESS_KEY)
                .processInstanceBusinessKey(request.debtId())
                .singleResult();

        if (existing != null) {
            log.info("Process already started pid={} debtId={}", existing.getId(), request.debtId());
            return new StartProcessResponse(existing.getId(), existing.getBusinessKey(), false);
        }

        Map<String, Object> vars = buildVariables(request, tenantId);

        ProcessInstance instance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey(PROCESS_KEY)
                .businessKey(request.debtId())
                .variables(vars)
                .start();

        log.info("Started process pid={} debtId={} tenantId={}", instance.getId(), request.debtId(), tenantId);
        return new StartProcessResponse(instance.getId(), instance.getBusinessKey(), true);
    }

    public List<TaskDto> getActiveTasks() {
        String tenantId = TenantContext.getRequiredTenantId();
        // Tasks belong to tenantless process instances (see startProcess), so — like every other
        // per-tenant Flowable query in this class — filter on the tenantId process variable
        // instead of the native (unset) Flowable tenant.
        return taskService.createTaskQuery()
                .processVariableValueEquals("tenantId", tenantId)
                .active()
                .list()
                .stream()
                .map(this::toTaskDto)
                .toList();
    }

    public void assignTask(String taskId, String assignee) {
        TenantContext.getRequiredTenantId();
        taskService.setAssignee(taskId, assignee);
        log.info("Assigned taskId={} assignee={}", taskId, assignee);
    }

    public void completeTask(String taskId, Map<String, Object> variables) {
        String tenantId = TenantContext.getRequiredTenantId();
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        String debtId = task != null ? String.valueOf(taskService.getVariable(taskId, "debtId")) : null;
        taskService.complete(taskId, variables != null ? variables : Map.of());
        log.info("Completed taskId={}", taskId);
        if (debtId != null) {
            outboxEventWriter.write(tenantId, "CollectionCase", debtId, "TaskActionPerformed",
                    Map.of("taskId", taskId, "taskName", task.getName(),
                            "actionType", variables != null ? String.valueOf(variables.getOrDefault("lastActionType", "COMPLETE")) : "COMPLETE"));
        }
    }

    /**
     * Business action (doc §17) with an agent-triggered email (doc §20) — sends the formal-notice
     * template before completing the task, using the task's own runtime variables (debtId,
     * customerEmail, etc. already set by stageRecovery). Email failure never blocks completion.
     */
    public void formalNoticeAction(String taskId, String notes) {
        TenantContext.getRequiredTenantId();
        Map<String, Object> taskVars = taskService.getVariables(taskId);
        sendActionEmail(taskVars, "formal-notice",
                "Formal Notice - Collection Case " + taskVars.get("debtId"), "AGENT_TRIGGERED");

        Map<String, Object> vars = new HashMap<>();
        vars.put("formalNoticeSentAt", System.currentTimeMillis());
        vars.put("formalNoticeNotes", notes);
        vars.put("lastActionType", "FORMAL_NOTICE");
        taskService.complete(taskId, vars);
        log.info("Formal notice sent and completed taskId={}", taskId);
    }

    /** Business action + agent-triggered escalation email (doc §20/§23). */
    public void escalateAction(String taskId, String reason) {
        TenantContext.getRequiredTenantId();
        Map<String, Object> taskVars = taskService.getVariables(taskId);
        Map<String, String> emailVars = new HashMap<>();
        emailVars.put("caseId", String.valueOf(taskVars.get("debtId")));
        emailVars.put("customerName", String.valueOf(taskVars.getOrDefault("customerName", taskVars.get("debtId"))));
        emailVars.put("escalationReason", reason);
        emailVars.put("escalationTarget", " to the senior collection team");
        String tenantId = String.valueOf(taskVars.get("tenantId"));
        String email = (String) taskVars.get("customerEmail");
        if (email != null) {
            notificationClient.sendEmail(tenantId, email,
                    "Collection Case Escalated to Legal", "escalation", emailVars, "AGENT_TRIGGERED");
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("escalationRequested", true);
        vars.put("escalationReason", reason);
        vars.put("lastActionType", "ESCALATE");
        taskService.complete(taskId, vars);
        log.info("Escalation email sent and completed taskId={}", taskId);
    }

    private void sendActionEmail(Map<String, Object> taskVars, String templateName, String subject, String triggerType) {
        String email = (String) taskVars.get("customerEmail");
        if (email == null) {
            return;
        }
        String tenantId = String.valueOf(taskVars.get("tenantId"));
        Map<String, String> emailVars = Map.of(
                "caseId", String.valueOf(taskVars.get("debtId")),
                "customerName", String.valueOf(taskVars.getOrDefault("customerName", taskVars.get("debtId"))),
                "amount", String.valueOf(taskVars.get("totalExposure")),
                "daysOverdue", String.valueOf(taskVars.get("overdueDays")));
        notificationClient.sendEmail(tenantId, email, subject, templateName, emailVars, triggerType);
    }

    /**
     * External payment event (doc §22): resumes bankingRecoveryProcess's
     * {@code paymentReceivedMessage} intermediate catch event via proper Flowable message
     * correlation — replaces the earlier demo workaround of setting {@code paymentReceived} and
     * manually firing the 7-day timer job, which raced the async executor and 500'd once already.
     * Flowable 7.2's {@code RuntimeService} has no fluent message-correlation builder, so this
     * uses the classic pair: find the waiting execution by its message subscription name (scoped
     * to the business key so a collision with another process definition can't misroute it), then
     * {@code messageEventReceived}.
     */
    public void correlatePayment(String caseId, Double amount, String paymentDate) {
        TenantContext.getRequiredTenantId();
        List<Execution> waiting = runtimeService.createExecutionQuery()
                .messageEventSubscriptionName("paymentReceivedMessage")
                .list();
        Execution execution = waiting.stream()
                .filter(e -> caseId.equals(runtimeService.getVariable(e.getProcessInstanceId(), "debtId")))
                .findFirst()
                .orElse(null);
        if (execution == null) {
            throw new IllegalStateException("No process instance for caseId " + caseId
                    + " is currently waiting on paymentReceivedMessage");
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put("paymentAmount", amount);
        vars.put("paymentDate", paymentDate);
        vars.put("paymentReceived", true);

        String tenantId = (String) runtimeService.getVariable(execution.getProcessInstanceId(), "tenantId");
        String customerEmail = (String) runtimeService.getVariable(execution.getProcessInstanceId(), "customerEmail");
        if (customerEmail != null) {
            Map<String, String> emailVars = Map.of(
                    "caseId", caseId,
                    "customerName", String.valueOf(runtimeService.getVariable(
                            execution.getProcessInstanceId(), "customerName")),
                    "amount", String.valueOf(amount),
                    "promiseDate", String.valueOf(paymentDate));
            notificationClient.sendEmail(tenantId, customerEmail, "Payment Promise Confirmation - Collection Case " + caseId,
                    "payment-confirmation", emailVars, "AUTOMATIC");
        }

        runtimeService.messageEventReceived("paymentReceivedMessage", execution.getId(), vars);
        log.info("Payment correlated caseId={} amount={} paymentDate={}", caseId, amount, paymentDate);
    }

    private TaskDto toTaskDto(Task t) {
        return new TaskDto(t.getId(), t.getName(), t.getProcessInstanceId(),
                t.getProcessDefinitionId(), t.getAssignee(), t.getCreateTime());
    }

    // Process instances are never tagged with a native Flowable tenant (see startProcess); the
    // tenant boundary for a single processInstanceId is that it was only ever handed back to the
    // tenant that started it, so these per-instance lookups don't re-filter by tenant.

    public List<ProcessHistoryDto> getProcessHistory(String processInstanceId) {
        TenantContext.getRequiredTenantId();
        return historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list()
                .stream()
                .map(h -> new ProcessHistoryDto(h.getActivityId(), h.getActivityName(),
                        h.getActivityType(), h.getStartTime(), h.getEndTime(), h.getDurationInMillis()))
                .toList();
    }

    /**
     * Generic, process-agnostic variable setter (mirrors getVariables below) — e.g. to flip
     * bankingRecoveryProcess's {@code paymentReceived} before its 7-day timer fires. Works on any
     * currently-active process instance, not just debtCollectionProcess.
     */
    public void setVariables(String processInstanceId, Map<String, Object> variables) {
        TenantContext.getRequiredTenantId();
        runtimeService.setVariables(processInstanceId, variables != null ? variables : Map.of());
        log.info("Variables set pid={} names={}", processInstanceId,
                variables != null ? variables.keySet() : Map.of().keySet());
    }

    public Map<String, Object> getVariables(String processInstanceId) {
        TenantContext.getRequiredTenantId();
        // The current BPMN has no wait states, so by the time a caller asks, the instance has
        // usually already completed and left the runtime tables — read from history instead.
        return historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list()
                .stream()
                .collect(Collectors.toMap(
                        HistoricVariableInstance::getVariableName,
                        HistoricVariableInstance::getValue));
    }

    public List<IncidentDto> getIncidents(String processInstanceId) {
        TenantContext.getRequiredTenantId();
        return managementService.createJobQuery()
                .processInstanceId(processInstanceId)
                .withException()
                .list()
                .stream()
                .map(j -> new IncidentDto(j.getId(), j.getProcessInstanceId(), j.getElementId(),
                        "failedJob", j.getExceptionMessage(), j.getDuedate()))
                .toList();
    }

    public List<JobDto> getDeadLetterJobs(String processInstanceId) {
        TenantContext.getRequiredTenantId();
        return managementService.createDeadLetterJobQuery()
                .processInstanceId(processInstanceId)
                .list()
                .stream()
                .map(j -> new JobDto(j.getId(), j.getProcessInstanceId(), j.getElementId(),
                        j.getElementName(), j.getDuedate(), j.getRetries(), j.getExceptionMessage()))
                .toList();
    }

    public void retryDeadLetterJob(String jobId) {
        TenantContext.getRequiredTenantId();
        managementService.moveDeadLetterJobToExecutableJob(jobId, 3);
        log.info("Dead-letter job requeued jobId={}", jobId);
    }

    public List<JobDto> getTimers(String processInstanceId) {
        TenantContext.getRequiredTenantId();
        return managementService.createTimerJobQuery()
                .processInstanceId(processInstanceId)
                .list()
                .stream()
                .map(j -> new JobDto(j.getId(), j.getProcessInstanceId(), j.getElementId(),
                        j.getElementName(), j.getDuedate(), j.getRetries(), j.getExceptionMessage()))
                .toList();
    }

    public void triggerTimer(String timerId) {
        TenantContext.getRequiredTenantId();
        Job executableJob = managementService.moveTimerToExecutableJob(timerId);
        managementService.executeJob(executableJob.getId());
        log.info("Timer triggered timerId={}", timerId);
    }
}
