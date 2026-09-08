package com.mercure.recouvrement.workflow.client;

/**
 * Response returned by segmentation-service's collection-qualification DMN.
 * Mirrors that service's {@code DecisionResponse}.
 */
public record SegmentationDecisionResponse(
        String debtId,
        String qualification,
        String qualificationReason,
        boolean simulated
) {}
