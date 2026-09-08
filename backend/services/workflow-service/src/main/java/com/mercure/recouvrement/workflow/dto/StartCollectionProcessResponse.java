package com.mercure.recouvrement.workflow.dto;

public record StartCollectionProcessResponse(
        String processInstanceId,
        String debtId,
        String customerId,
        String qualification,
        String qualificationReason,
        String segment,
        String strategy,
        String status
) {}
