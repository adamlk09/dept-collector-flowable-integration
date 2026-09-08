package com.deptcollector.segmentation.api.dto;

public record StagingDecisionResponse(
        String debtId,
        String stage,
        String priority,
        String action,
        boolean simulated,

        // Milestone 2 — additive, from the 4-decision chain (CollectionDecisionService)
        String ifrsStage,
        String collectionPhase,
        int priorityScore,
        String recommendedAction,
        String collectorLevel,
        String customerSegment
) {}
