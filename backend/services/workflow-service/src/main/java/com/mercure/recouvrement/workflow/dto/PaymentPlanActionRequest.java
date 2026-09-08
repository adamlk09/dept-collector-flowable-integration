package com.mercure.recouvrement.workflow.dto;

public record PaymentPlanActionRequest(
        Double amount,
        Integer installments
) {}
