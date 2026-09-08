package com.deptcollector.segmentation.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Behavioral input for the collection-qualification DMN (no numeric scoring).
 *
 * <p>The categorical dimensions are the results of EPIC 1–6 (see workflow-service
 * doc/segmentation.md). Every categorical field is optional — the DMN's default rule
 * ({@code FIRST_CONTACT}) covers any combination the caller leaves unset.
 */
public record DecisionRequest(
        @NotBlank String debtId,
        String clientInfoStatus,
        String contractStatus,
        String debtStatus,
        String paymentHistory,
        String collectionStage,
        String promiseStatus,
        Boolean reachable
) {}
