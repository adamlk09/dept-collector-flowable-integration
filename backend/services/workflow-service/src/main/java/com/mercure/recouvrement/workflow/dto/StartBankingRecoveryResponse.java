package com.mercure.recouvrement.workflow.dto;

public record StartBankingRecoveryResponse(
        String processInstanceId,
        String debtId,
        String customerId,
        String stage,
        String priority,
        String action,
        String recoveryTeam
) {}
