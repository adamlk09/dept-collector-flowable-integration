package com.mercure.recouvrement.workflow.application;

import com.deptcollector.shared.tenant.TenantContext;
import com.mercure.recouvrement.workflow.dto.StartBankingRecoveryRequest;
import com.mercure.recouvrement.workflow.dto.StartBankingRecoveryResponse;
import com.mercure.recouvrement.workflow.dto.StartProcessResponse;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Second, independent process's entry point (see doc on bankingRecoveryProcess). Mirrors
 * CollectionWorkflowService's start/idempotency/tenant-as-variable patterns exactly; task and
 * history/variable/incident/job/timer operations stay solely in CollectionWorkflowService,
 * which is already process-agnostic (keyed by processInstanceId/taskId, not process key).
 */
@Service
public class BankingRecoveryWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(BankingRecoveryWorkflowService.class);
    private static final String PROCESS_KEY = "bankingRecoveryProcess";

    private final RuntimeService runtimeService;
    private final HistoryService historyService;

    public BankingRecoveryWorkflowService(RuntimeService runtimeService, HistoryService historyService) {
        this.runtimeService = runtimeService;
        this.historyService = historyService;
    }

    /**
     * Synchronous entry point. Starts bankingRecoveryProcess, which runs in the calling thread
     * up to its one wait state — the {@code assignToAgent} user task — then returns with
     * {@code stage}/{@code priority}/{@code action}/{@code recoveryTeam} already resolved by
     * stageRecovery.
     */
    public StartBankingRecoveryResponse startRecovery(StartBankingRecoveryRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();

        Map<String, Object> vars = buildVariables(request, tenantId);

        // The BPMN auto-deploys to the default (tenantless) deployment (see
        // CollectionWorkflowService), so the process is started without a native tenant; the
        // tenant is retained as a process variable for traceability.
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                PROCESS_KEY, request.debtId(), vars);

        String pid = instance.getId();
        log.info("Banking recovery process reached agent task pid={} debtId={} tenantId={}",
                pid, request.debtId(), tenantId);

        String stage = (String) requireHistoricVariable(pid, "stage");
        String priority = (String) requireHistoricVariable(pid, "priority");
        String action = (String) requireHistoricVariable(pid, "action");
        String recoveryTeam = (String) requireHistoricVariable(pid, "recoveryTeam");

        return new StartBankingRecoveryResponse(
                pid, request.debtId(), request.customerId(), stage, priority, action, recoveryTeam);
    }

    /**
     * Idempotent start: dedups against history (not runtime — the process has a wait state, and
     * a retry could land after the instance moved further/completed), same as
     * CollectionWorkflowService.startProcess.
     */
    public StartProcessResponse startProcess(StartBankingRecoveryRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();

        HistoricProcessInstance existing = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(PROCESS_KEY)
                .processInstanceBusinessKey(request.debtId())
                .singleResult();

        if (existing != null) {
            log.info("Banking recovery process already started pid={} debtId={}", existing.getId(), request.debtId());
            return new StartProcessResponse(existing.getId(), existing.getBusinessKey(), false);
        }

        Map<String, Object> vars = buildVariables(request, tenantId);

        ProcessInstance instance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey(PROCESS_KEY)
                .businessKey(request.debtId())
                .variables(vars)
                .start();

        log.info("Started banking recovery process pid={} debtId={} tenantId={}",
                instance.getId(), request.debtId(), tenantId);
        return new StartProcessResponse(instance.getId(), instance.getBusinessKey(), true);
    }

    private Map<String, Object> buildVariables(StartBankingRecoveryRequest request, String tenantId) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("debtId", request.debtId());
        vars.put("customerId", request.customerId());
        vars.put("clientSegment", orDefault(request.clientSegment(), "PARTICULIERS"));
        vars.put("overdueDays", request.overdueDays());
        vars.put("totalExposure", request.totalExposure());
        vars.put("phoneNumber", request.phoneNumber());
        vars.put("address", request.address());
        vars.put("customerEmail", orDefault(request.customerEmail(), request.customerId() + "@demo.deptcollector.local"));
        vars.put("customerName", orDefault(request.customerName(), request.customerId()));
        vars.put("tenantId", tenantId);
        // Gateway conditions (${escalationRequested == true}, ${paymentReceived == true}) throw
        // PropertyNotFoundException on a variable that was never set, rather than treating it as
        // null/false — default both up front so any business action that doesn't set them
        // (promise-payment, contact-customer, etc.) still lets the process evaluate cleanly.
        vars.put("escalationRequested", false);
        vars.put("paymentReceived", false);
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
}
