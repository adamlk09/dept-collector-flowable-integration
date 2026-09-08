package com.mercure.recouvrement.workflow.delegate;

import com.mercure.recouvrement.workflow.audit.OutboxEventWriter;
import com.mercure.recouvrement.workflow.client.CaseServiceClient;
import com.mercure.recouvrement.workflow.client.NotificationClient;
import com.mercure.recouvrement.workflow.client.SegmentationClient;
import com.mercure.recouvrement.workflow.client.StagingDecisionRequest;
import com.mercure.recouvrement.workflow.client.StagingDecisionResponse;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Step 3 of bankingRecoveryProcess: automatic staging by ageing (IFRS 9-style) and exposure.
 *
 * <p>Pipeline mirrors {@link SegmentDebtDelegate}: call segmentation-service's 4-decision chain
 * (customer segmentation / staging / priority / action, orchestrated server-side by
 * CollectionDecisionService), falling back to a local engine if the call fails — never throws.
 * Sets {@code stage}, {@code priority}, {@code action}, {@code recoveryTeam} (the established,
 * BPMN-facing variables the gateway/user task already depend on) plus, additively since
 * Milestone 2, the doc's richer vocabulary: {@code ifrsStage}, {@code collectionPhase},
 * {@code priorityScore}, {@code recommendedAction}, {@code collectorLevel}, {@code customerSegment}.
 */
@Component("stageRecoveryDelegate")
public class StageRecoveryDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(StageRecoveryDelegate.class);

    private final SegmentationClient segmentationClient;
    private final CaseServiceClient caseServiceClient;
    private final NotificationClient notificationClient;
    private final OutboxEventWriter outboxEventWriter;

    public StageRecoveryDelegate(SegmentationClient segmentationClient, CaseServiceClient caseServiceClient,
                                 NotificationClient notificationClient, OutboxEventWriter outboxEventWriter) {
        this.segmentationClient = segmentationClient;
        this.caseServiceClient = caseServiceClient;
        this.notificationClient = notificationClient;
        this.outboxEventWriter = outboxEventWriter;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String debtId = (String) execution.getVariable("debtId");
        String tenantId = (String) execution.getVariable("tenantId");
        Integer overdueDays = (Integer) execution.getVariable("overdueDays");
        Double totalExposure = (Double) execution.getVariable("totalExposure");
        String clientSegment = asString(execution.getVariable("clientSegment"), "PARTICULIERS");
        String customerType = toCustomerType(clientSegment);

        Staging staging;
        String source;
        try {
            StagingDecisionResponse response = segmentationClient.stageRecovery(
                    tenantId, MDC.get("correlationId"),
                    new StagingDecisionRequest(debtId, overdueDays, totalExposure, customerType));
            if (response == null || response.stage() == null) {
                throw new IllegalStateException("segmentation-service returned an empty staging result");
            }
            staging = new Staging(response.stage(), response.priority(), response.action(),
                    response.ifrsStage(), response.collectionPhase(), response.priorityScore(),
                    response.recommendedAction(), response.collectorLevel(), response.customerSegment());
            source = "DMN";
        } catch (Exception e) {
            log.warn("Staging DMN unavailable for pid={} debtId={} — falling back to local engine: {}",
                    processInstanceId, debtId, e.getMessage());
            staging = stageLocally(overdueDays, totalExposure, customerType);
            source = "LOCAL";
        }

        String recoveryTeam = "recoveryTeam-" + clientSegment;

        execution.setVariable("clientSegment", clientSegment);
        execution.setVariable("stage", staging.stage);
        execution.setVariable("priority", staging.priority);
        execution.setVariable("action", staging.action);
        execution.setVariable("recoveryTeam", recoveryTeam);
        execution.setVariable("stagingSource", source);
        execution.setVariable("stagedAt", System.currentTimeMillis());

        execution.setVariable("ifrsStage", staging.ifrsStage);
        execution.setVariable("collectionPhase", staging.collectionPhase);
        execution.setVariable("priorityScore", staging.priorityScore);
        execution.setVariable("recommendedAction", staging.recommendedAction);
        execution.setVariable("collectorLevel", staging.collectorLevel);
        execution.setVariable("customerSegment", staging.customerSegment);

        log.info("Debt staged pid={} source={} stage={} priority={} action={} recoveryTeam={} "
                        + "ifrsStage={} collectionPhase={} priorityScore={} recommendedAction={} "
                        + "collectorLevel={} customerSegment={}",
                processInstanceId, source, staging.stage, staging.priority, staging.action, recoveryTeam,
                staging.ifrsStage, staging.collectionPhase, staging.priorityScore, staging.recommendedAction,
                staging.collectorLevel, staging.customerSegment);

        // doc §12/§30: persist the durable case snapshot in case-service, outside Flowable's own
        // tables. Best-effort — never blocks staging.
        caseServiceClient.createOrGetCase(tenantId, debtId, (String) execution.getVariable("customerId"),
                customerType, overdueDays, totalExposure != null ? BigDecimal.valueOf(totalExposure) : null,
                staging.ifrsStage, staging.customerSegment, staging.priority, processInstanceId);

        // doc §18-20: automatic email at case-creation time — high-exposure cases get the alert
        // template, everything else the plain reminder. Best-effort, never blocks staging.
        String customerEmail = asString(execution.getVariable("customerEmail"), null);
        if (customerEmail != null) {
            boolean highExposure = "HIGH".equals(staging.priority) || "MAXIMUM".equals(staging.priority);
            String templateName = highExposure ? "high-exposure-alert" : "collection-reminder";
            String subjectPrefix = highExposure ? "HIGH PRIORITY Collection Case " : "Payment Reminder - Collection Case ";
            notificationClient.sendEmail(tenantId, customerEmail, subjectPrefix + debtId, templateName,
                    Map.of(
                            "caseId", debtId,
                            "customerName", asString(execution.getVariable("customerName"), debtId),
                            "amount", String.valueOf(totalExposure),
                            "daysOverdue", String.valueOf(overdueDays)),
                    "AUTOMATIC");
        }

        outboxEventWriter.write(tenantId, "CollectionCase", debtId, "CaseStatusChanged", Map.of(
                "stage", staging.stage, "priority", staging.priority, "action", staging.action,
                "ifrsStage", staging.ifrsStage, "collectionPhase", staging.collectionPhase,
                "processInstanceId", processInstanceId));
    }

    /** recoveryTeam's plural French segment names -> the DMN's singular customerType vocabulary. */
    private static String toCustomerType(String clientSegment) {
        return switch (clientSegment) {
            case "ENTREPRISES" -> "ENTREPRISE";
            case "PROFESSIONNELS" -> "PROFESSIONNEL";
            default -> "PARTICULIER";
        };
    }

    /**
     * Local fallback mirroring the 4-decision chain: evaluates the same 5 ageing/exposure rules,
     * priority/action/segment lookups, entirely in-process — never-throw contract.
     */
    private Staging stageLocally(Integer overdueDays, Double totalExposure, String customerType) {
        int days = overdueDays != null ? overdueDays : 0;
        double exposure = totalExposure != null ? totalExposure : 0d;

        String ifrsStage;
        String collectionPhase;
        if (days >= 1 && days <= 30) {
            ifrsStage = "STAGE_2"; collectionPhase = "COMMERCIAL";
        } else if (days >= 31 && days <= 90 && exposure >= 15000) {
            ifrsStage = "STAGE_2"; collectionPhase = "HIGH_EXPOSURE";
        } else if (days >= 31 && days <= 60) {
            ifrsStage = "STAGE_2"; collectionPhase = "AMIABLE";
        } else if (days >= 61 && days <= 90) {
            ifrsStage = "STAGE_2"; collectionPhase = "PRE_CONTENTIOUS";
        } else {
            ifrsStage = "STAGE_3"; collectionPhase = "LEGAL";
        }

        String stage, priority, action, recommendedAction, collectorLevel;
        int priorityScore;
        switch (collectionPhase) {
            case "COMMERCIAL" -> {
                stage = "STAGE2_COMMERCIAL"; priority = "LOW"; action = "AUTO_REMINDER";
                priorityScore = 20; recommendedAction = "SMS_EMAIL"; collectorLevel = "COLLECTOR_LEVEL_1";
            }
            case "AMIABLE" -> {
                stage = "STAGE2_AMICABLE"; priority = "MEDIUM"; action = "PHONE_CALL_L1";
                priorityScore = 45; recommendedAction = "PHONE_CALL"; collectorLevel = "COLLECTOR_LEVEL_1";
            }
            case "HIGH_EXPOSURE" -> {
                stage = "STAGE2_SENSITIVE_HIGH_EXPOSURE"; priority = "HIGH"; action = "SENIOR_ALERT_FORMAL_NOTICE";
                priorityScore = 75; recommendedAction = "FORMAL_NOTICE"; collectorLevel = "SENIOR_COLLECTOR";
            }
            case "PRE_CONTENTIOUS" -> {
                stage = "STAGE2_PRE_LITIGATION"; priority = "MEDIUM"; action = "REGISTERED_LETTER";
                priorityScore = 55; recommendedAction = "PHONE_CALL"; collectorLevel = "COLLECTOR_LEVEL_1";
            }
            default -> {
                stage = "STAGE3_LITIGATION"; priority = "MAXIMUM"; action = "LEGAL_TRANSFER";
                priorityScore = 95; recommendedAction = "LEGAL_ESCALATION"; collectorLevel = "LEGAL_TEAM";
            }
        }

        String customerSegment = "PROFESSIONNEL".equals(customerType) && exposure >= 50000
                ? "ENTREPRISE" : customerType;

        return new Staging(stage, priority, action, ifrsStage, collectionPhase, priorityScore,
                recommendedAction, collectorLevel, customerSegment);
    }

    private static String asString(Object value, String fallback) {
        return value instanceof String s && !s.isBlank() ? s : fallback;
    }

    private record Staging(String stage, String priority, String action, String ifrsStage,
                            String collectionPhase, int priorityScore, String recommendedAction,
                            String collectorLevel, String customerSegment) {}
}
