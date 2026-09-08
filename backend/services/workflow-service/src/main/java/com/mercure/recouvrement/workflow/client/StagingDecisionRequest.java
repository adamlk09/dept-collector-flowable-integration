package com.mercure.recouvrement.workflow.client;

public record StagingDecisionRequest(
        String debtId,
        Integer overdueDays,
        Double totalExposure,
        String customerType
) {}
