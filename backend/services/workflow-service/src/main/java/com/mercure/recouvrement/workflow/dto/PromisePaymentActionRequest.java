package com.mercure.recouvrement.workflow.dto;

public record PromisePaymentActionRequest(
        Double amount,
        String promiseDate
) {}
