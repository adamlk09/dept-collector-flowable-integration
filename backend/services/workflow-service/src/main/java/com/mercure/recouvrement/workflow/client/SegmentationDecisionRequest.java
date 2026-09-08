package com.mercure.recouvrement.workflow.client;

/**
 * Payload sent to segmentation-service's collection-qualification DMN.
 * Mirrors that service's {@code DecisionRequest} (behavioral, no scoring).
 */
public record SegmentationDecisionRequest(
        String debtId,
        String clientInfoStatus,
        String contractStatus,
        String debtStatus,
        String paymentHistory,
        String collectionStage,
        String promiseStatus,
        Boolean reachable
) {}
