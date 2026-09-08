package com.mercure.recouvrement.workflow;

import com.deptcollector.shared.tenant.TenantContext;
import com.mercure.recouvrement.workflow.application.BankingRecoveryWorkflowService;
import com.mercure.recouvrement.workflow.dto.StartBankingRecoveryRequest;
import com.mercure.recouvrement.workflow.dto.StartBankingRecoveryResponse;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Doc §35: exercises bankingRecoveryProcess end-to-end against the real engine/DB — no
 * H2/Testcontainers per this service's Postgres-only convention (workflow-service/CLAUDE.md).
 * Requires `docker compose up -d` (Postgres) and no other test depends on ordering, so each test
 * uses its own unique debtId.
 *
 * <p>segmentation-service is NOT required to be running: {@code StageRecoveryDelegate}'s local
 * fallback mirrors the DMN chain's rules exactly, so the four canonical scenarios (doc §32)
 * assert identically whether the DMN call succeeds or falls back.
 */
@SpringBootTest
@ActiveProfiles("dev")
class BankingRecoveryProcessIntegrationTest {

    @Autowired
    private BankingRecoveryWorkflowService workflowService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @BeforeEach
    void setTenant() {
        TenantContext.set("tenant-test");
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void scenario1_commercial_lowPriority() {
        StartBankingRecoveryResponse response = start("IT-CASE-001", 21, 3000.0, "PARTICULIERS");

        assertThat(response.stage()).isEqualTo("STAGE2_COMMERCIAL");
        assertThat(response.priority()).isEqualTo("LOW");
        assertThat(response.action()).isEqualTo("AUTO_REMINDER");
    }

    @Test
    void scenario2_amicable_mediumPriority() {
        StartBankingRecoveryResponse response = start("IT-CASE-002", 45, 8000.0, "PARTICULIERS");

        assertThat(response.stage()).isEqualTo("STAGE2_AMICABLE");
        assertThat(response.priority()).isEqualTo("MEDIUM");
        assertThat(response.action()).isEqualTo("PHONE_CALL_L1");
    }

    @Test
    void scenario3_highExposure_seniorCollector() {
        StartBankingRecoveryResponse response = start("IT-CASE-003", 67, 18500.0, "PARTICULIERS");

        assertThat(response.stage()).isEqualTo("STAGE2_SENSITIVE_HIGH_EXPOSURE");
        assertThat(response.priority()).isEqualTo("HIGH");
        assertThat(response.action()).isEqualTo("SENIOR_ALERT_FORMAL_NOTICE");
        assertThat(response.recoveryTeam()).isEqualTo("recoveryTeam-PARTICULIERS");
    }

    @Test
    void scenario4_legal_criticalPriority_opensCmmnCase() {
        String debtId = "IT-CASE-004";
        StartBankingRecoveryResponse response = start(debtId, 120, 25000.0, "ENTREPRISES");

        assertThat(response.stage()).isEqualTo("STAGE3_LITIGATION");
        assertThat(response.priority()).isEqualTo("MAXIMUM");

        // Complete the human task so the process routes through startLegalCase (CMMN)
        String pid = response.processInstanceId();
        Task task = taskService.createTaskQuery().processInstanceId(pid).singleResult();
        taskService.complete(task.getId());

        // The Stage 3 branch has no further wait state, so the process instance may already be
        // gone from the runtime tables by now — read the resolved value from history instead.
        Object cmmnCaseInstanceId = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(pid).variableName("cmmnCaseInstanceId").singleResult().getValue();
        assertThat(cmmnCaseInstanceId).isNotNull();
    }

    @Test
    void taskLifecycle_claimAndPromisePayment() {
        StartBankingRecoveryResponse response = start("IT-CASE-005", 45, 8000.0, "PARTICULIERS");
        String pid = response.processInstanceId();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(pid).list();
        assertThat(tasks).hasSize(1);
        Task task = tasks.get(0);
        assertThat(task.getName()).isEqualTo("Handle Recovery Case");

        taskService.setAssignee(task.getId(), "agent.test");
        assertThat(taskService.createTaskQuery().taskId(task.getId()).singleResult().getAssignee())
                .isEqualTo("agent.test");

        taskService.complete(task.getId(), Map.of("paymentPromiseAmount", 8000.0, "paymentPromiseDate", "2026-12-01"));

        Object promiseAmount = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(pid).variableName("paymentPromiseAmount").singleResult().getValue();
        assertThat(promiseAmount).isEqualTo(8000.0);
    }

    @Test
    void escalation_bumpsCollectorLevelAndPriority() {
        StartBankingRecoveryResponse response = start("IT-CASE-006", 38, 6500.0, "PARTICULIERS");
        String pid = response.processInstanceId();

        Task task = taskService.createTaskQuery().processInstanceId(pid).singleResult();
        taskService.complete(task.getId(), Map.of("escalationRequested", true, "escalationReason", "Test escalation"));

        Task escalated = taskService.createTaskQuery().processInstanceId(pid).singleResult();
        assertThat(escalated.getName()).isEqualTo("Handle Escalated Case (Senior)");
        assertThat(runtimeService.getVariable(pid, "collectorLevel")).isEqualTo("SENIOR_COLLECTOR");
        assertThat(runtimeService.getVariable(pid, "priority")).isEqualTo("HIGH");
    }

    private StartBankingRecoveryResponse start(String debtId, int overdueDays, double totalExposure, String clientSegment) {
        return workflowService.startRecovery(new StartBankingRecoveryRequest(
                debtId, "CUST-" + debtId, clientSegment, overdueDays, totalExposure,
                "+212600000000", "Test Address", null, null));
    }
}
