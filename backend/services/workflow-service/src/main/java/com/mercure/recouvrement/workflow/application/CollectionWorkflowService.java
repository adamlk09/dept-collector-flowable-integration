package com.mercure.recouvrement.workflow.application;

import com.deptcollector.shared.tenant.TenantContext;
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
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CollectionWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(CollectionWorkflowService.class);
    private static final String PROCESS_KEY = "debtCollectionProcess";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final ManagementService managementService;

    public CollectionWorkflowService(RuntimeService runtimeService,
                                     TaskService taskService,
                                     HistoryService historyService,
                                     ManagementService managementService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
        this.managementService = managementService;
    }

    /**
     * Synchronous debt-collection entry point. Starts the BPMN process (which has no
     * wait states, so it runs to its end event in the calling thread), then reads the
     * outcome back from history. Decision logic lives entirely in the BPMN delegates and
     * gateway — never here.
     */
    public StartCollectionProcessResponse startCollectionProcess(StartCollectionProcessRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();

        Map<String, Object> vars = new HashMap<>();
        vars.put("debtId", request.debtId());
        vars.put("customerId", request.customerId());
        vars.put("score", request.score());
        vars.put("tenantId", tenantId);

        // The BPMN is auto-deployed to the default (tenantless) deployment, so the process
        // is started without a tenant; the tenant is retained as a process variable for traceability.
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                PROCESS_KEY, request.debtId(), vars);

        String pid = instance.getId();
        log.info("Collection process completed pid={} debtId={} tenantId={}", pid, request.debtId(), tenantId);

        String segment = (String) requireHistoricVariable(pid, "segment");
        String strategy = (String) requireHistoricVariable(pid, "strategy");
        String status = (String) requireHistoricVariable(pid, "status");

        return new StartCollectionProcessResponse(
                pid, request.debtId(), request.customerId(), request.score(), segment, strategy, status);
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

    public StartProcessResponse startProcess(StartCollectionProcessRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();

        ProcessInstance existing = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESS_KEY)
                .processInstanceBusinessKey(request.debtId())
                .processInstanceTenantId(tenantId)
                .singleResult();

        if (existing != null) {
            log.info("Process already running pid={} debtId={}", existing.getId(), request.debtId());
            return new StartProcessResponse(existing.getId(), existing.getBusinessKey(), false);
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("debtId", request.debtId());
        vars.put("customerId", request.customerId());
        vars.put("score", request.score());
        vars.put("tenantId", tenantId);

        ProcessInstance instance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey(PROCESS_KEY)
                .businessKey(request.debtId())
                .tenantId(tenantId)
                .variables(vars)
                .start();

        log.info("Started process pid={} debtId={} tenantId={}", instance.getId(), request.debtId(), tenantId);
        return new StartProcessResponse(instance.getId(), instance.getBusinessKey(), true);
    }

    public List<TaskDto> getActiveTasks() {
        String tenantId = TenantContext.getRequiredTenantId();
        return taskService.createTaskQuery()
                .taskTenantId(tenantId)
                .active()
                .list()
                .stream()
                .map(t -> new TaskDto(t.getId(), t.getName(), t.getProcessInstanceId(),
                        t.getProcessDefinitionId(), t.getCreateTime()))
                .toList();
    }

    public void completeTask(String taskId, Map<String, Object> variables) {
        TenantContext.getRequiredTenantId();
        taskService.complete(taskId, variables != null ? variables : Map.of());
        log.info("Completed taskId={}", taskId);
    }

    public List<ProcessHistoryDto> getProcessHistory(String processInstanceId) {
        String tenantId = TenantContext.getRequiredTenantId();
        return historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .activityTenantId(tenantId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list()
                .stream()
                .map(h -> new ProcessHistoryDto(h.getActivityId(), h.getActivityName(),
                        h.getActivityType(), h.getStartTime(), h.getEndTime(), h.getDurationInMillis()))
                .toList();
    }

    public Map<String, Object> getVariables(String processInstanceId) {
        TenantContext.getRequiredTenantId();
        return runtimeService.getVariables(processInstanceId);
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
