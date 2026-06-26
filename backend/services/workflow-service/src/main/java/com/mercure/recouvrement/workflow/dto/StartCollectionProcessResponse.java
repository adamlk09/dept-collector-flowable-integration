package com.mercure.recouvrement.workflow.dto;

public record StartCollectionProcessResponse(
        String processInstanceId,
        String debtId,
        String customerId,
        Integer score,
        String segment,
        String strategy,
        String status
) {}
