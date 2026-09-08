package com.mercure.recouvrement.workflow.client;

public record StagingDecisionResponse(
        String debtId,
        String stage,
        String priority,
        String action,
        boolean simulated,
        String ifrsStage,
        String collectionPhase,
        int priorityScore,
        String recommendedAction,
        String collectorLevel,
        String customerSegment
) {}
