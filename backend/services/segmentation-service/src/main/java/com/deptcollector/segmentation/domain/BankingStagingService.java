package com.deptcollector.segmentation.domain;

import com.deptcollector.segmentation.api.dto.StagingDecisionRequest;
import com.deptcollector.segmentation.api.dto.StagingDecisionResponse;
import com.deptcollector.shared.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * bankingRecoveryProcess's staging endpoint. As of Milestone 2 this delegates to
 * {@link CollectionDecisionService}'s 4-decision chain (collectionStagingDecision ->
 * collectionPriorityDecision -> collectionActionDecision -> customerSegmentationDecision) instead
 * of the single combined {@code bankingStagingDecision} table it originally called (removed).
 *
 * <p>The response keeps its original {@code stage}/{@code priority}/{@code action} fields and
 * values byte-for-byte identical (mapped from the chain's {@code ifrsStage}/{@code collectionPhase})
 * so {@code StageRecoveryDelegate} and the BPMN gateway condition
 * {@code ${stage == 'STAGE3_LITIGATION'}} need no changes — the richer fields
 * (ifrsStage/collectionPhase/priorityScore/collectorLevel/customerSegment/recommendedAction) are
 * purely additive.
 */
@Service
public class BankingStagingService {

    private static final Logger log = LoggerFactory.getLogger(BankingStagingService.class);

    private final CollectionDecisionService collectionDecisionService;

    public BankingStagingService(CollectionDecisionService collectionDecisionService) {
        this.collectionDecisionService = collectionDecisionService;
    }

    public StagingDecisionResponse execute(StagingDecisionRequest request) {
        return evaluate(request, false);
    }

    public StagingDecisionResponse simulate(StagingDecisionRequest request) {
        return evaluate(request, true);
    }

    private StagingDecisionResponse evaluate(StagingDecisionRequest request, boolean simulated) {
        String tenantId = TenantContext.getRequiredTenantId();

        log.info("Staging evaluate debtId={} overdueDays={} totalExposure={} customerType={} tenantId={} simulated={}",
                request.debtId(), request.overdueDays(), request.totalExposure(), request.customerType(),
                tenantId, simulated);

        CollectionDecisionService.Result result = collectionDecisionService.evaluate(
                request.overdueDays(), request.totalExposure(),
                request.customerType() != null && !request.customerType().isBlank()
                        ? request.customerType() : "PARTICULIER");

        String stage = legacyStage(result.collectionPhase());
        String priority = legacyPriority(result.collectionPhase());
        String action = legacyAction(result.collectionPhase());

        log.info("Staging result debtId={} stage={} priority={} action={} ifrsStage={} collectionPhase={} "
                        + "priorityScore={} recommendedAction={} collectorLevel={} customerSegment={}",
                request.debtId(), stage, priority, action, result.ifrsStage(), result.collectionPhase(),
                result.priorityScore(), result.recommendedAction(), result.collectorLevel(), result.customerSegment());

        return new StagingDecisionResponse(request.debtId(), stage, priority, action, simulated,
                result.ifrsStage(), result.collectionPhase(), result.priorityScore(),
                result.recommendedAction(), result.collectorLevel(), result.customerSegment());
    }

    /** Maps the doc's collectionPhase vocabulary back to the already-deployed/demoed stage names. */
    private static String legacyStage(String collectionPhase) {
        return switch (collectionPhase) {
            case "COMMERCIAL" -> "STAGE2_COMMERCIAL";
            case "AMIABLE" -> "STAGE2_AMICABLE";
            case "HIGH_EXPOSURE" -> "STAGE2_SENSITIVE_HIGH_EXPOSURE";
            case "PRE_CONTENTIOUS" -> "STAGE2_PRE_LITIGATION";
            case "LEGAL" -> "STAGE3_LITIGATION";
            default -> "STAGE2_COMMERCIAL";
        };
    }

    private static String legacyPriority(String collectionPhase) {
        return switch (collectionPhase) {
            case "COMMERCIAL" -> "LOW";
            case "AMIABLE", "PRE_CONTENTIOUS" -> "MEDIUM";
            case "HIGH_EXPOSURE" -> "HIGH";
            case "LEGAL" -> "MAXIMUM";
            default -> "LOW";
        };
    }

    private static String legacyAction(String collectionPhase) {
        return switch (collectionPhase) {
            case "COMMERCIAL" -> "AUTO_REMINDER";
            case "AMIABLE" -> "PHONE_CALL_L1";
            case "HIGH_EXPOSURE" -> "SENIOR_ALERT_FORMAL_NOTICE";
            case "PRE_CONTENTIOUS" -> "REGISTERED_LETTER";
            case "LEGAL" -> "LEGAL_TRANSFER";
            default -> "AUTO_REMINDER";
        };
    }
}
