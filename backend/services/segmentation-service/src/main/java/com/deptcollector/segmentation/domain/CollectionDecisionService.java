package com.deptcollector.segmentation.domain;

import org.flowable.dmn.api.DmnDecisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Milestone 2: orchestrates the 4-decision DMN chain (customer segmentation, staging, priority,
 * action) behind a single call, so callers (workflow-service's StageRecoveryDelegate) still make
 * one outbound request and its never-throw fallback contract stays intact. Chaining lives here,
 * in segmentation-service, per the architecture ("DMN logic belongs where DMN lives") — never
 * duplicated as Java business rules.
 *
 * <p>Each decision consumes what earlier ones produced rather than re-deriving from raw inputs
 * (collection-priority.dmn reads {@code collectionPhase}; collection-action.dmn reads both
 * {@code priority} and {@code collectionPhase} — priority alone collapses AMIABLE and
 * PRE_CONTENTIOUS into the same MEDIUM bucket, which would lose the distinct action the business
 * matrix requires for each) to demonstrate genuine DMN chaining rather than four independent
 * lookups.
 */
@Service
public class CollectionDecisionService {

    private static final Logger log = LoggerFactory.getLogger(CollectionDecisionService.class);

    private final DmnDecisionService dmnDecisionService;

    public CollectionDecisionService(DmnDecisionService dmnDecisionService) {
        this.dmnDecisionService = dmnDecisionService;
    }

    public Result evaluate(Integer overdueDays, Double totalExposure, String customerType) {
        Map<String, Object> staging = dmnDecisionService.createExecuteDecisionBuilder()
                .decisionKey("collectionStagingDecision")
                .variable("overdueDays", overdueDays)
                .variable("totalExposure", totalExposure)
                .executeWithSingleResult();

        String ifrsStage = staging != null ? (String) staging.get("ifrsStage") : "STAGE_2";
        String collectionPhase = staging != null ? (String) staging.get("collectionPhase") : "COMMERCIAL";

        Map<String, Object> priorityResult = dmnDecisionService.createExecuteDecisionBuilder()
                .decisionKey("collectionPriorityDecision")
                .variable("collectionPhase", collectionPhase)
                .executeWithSingleResult();

        String priority = priorityResult != null ? (String) priorityResult.get("priority") : "LOW";
        Number priorityScore = priorityResult != null ? (Number) priorityResult.get("priorityScore") : 10;

        Map<String, Object> actionResult = dmnDecisionService.createExecuteDecisionBuilder()
                .decisionKey("collectionActionDecision")
                .variable("priority", priority)
                .variable("collectionPhase", collectionPhase)
                .executeWithSingleResult();

        String recommendedAction = actionResult != null ? (String) actionResult.get("recommendedAction") : "SMS_EMAIL";
        String collectorLevel = actionResult != null ? (String) actionResult.get("collectorLevel") : "COLLECTOR_LEVEL_1";

        Map<String, Object> segmentationResult = dmnDecisionService.createExecuteDecisionBuilder()
                .decisionKey("customerSegmentationDecision")
                .variable("customerType", customerType)
                .variable("totalExposure", totalExposure)
                .executeWithSingleResult();

        String customerSegment = segmentationResult != null
                ? (String) segmentationResult.get("customerSegment") : "PARTICULIER";

        log.info("Collection decision chain: overdueDays={} totalExposure={} customerType={} "
                        + "-> ifrsStage={} collectionPhase={} priority={} priorityScore={} "
                        + "recommendedAction={} collectorLevel={} customerSegment={}",
                overdueDays, totalExposure, customerType, ifrsStage, collectionPhase, priority,
                priorityScore, recommendedAction, collectorLevel, customerSegment);

        return new Result(ifrsStage, collectionPhase, priority, priorityScore.intValue(),
                recommendedAction, collectorLevel, customerSegment);
    }

    public record Result(
            String ifrsStage,
            String collectionPhase,
            String priority,
            int priorityScore,
            String recommendedAction,
            String collectorLevel,
            String customerSegment
    ) {}
}
