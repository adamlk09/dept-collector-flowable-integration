package com.mercure.recouvrement.workflow.dto;

public record PaymentRequest(
        Double amount,
        String paymentDate
) {}
