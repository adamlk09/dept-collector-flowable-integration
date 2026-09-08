package com.mercure.recouvrement.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Input for the banking recovery process (irregular account -> staging -> agent dispatch).
 * Staging is pure ageing (overdueDays) + exposure (totalExposure) — see doc on
 * bankingRecoveryProcess / bankingStagingDecision.
 */
public record StartBankingRecoveryRequest(
        @NotBlank String debtId,
        @NotBlank String customerId,

        // PARTICULIERS | PROFESSIONNELS | ENTREPRISES — drives the assignToAgent candidate group
        String clientSegment,

        @NotNull @PositiveOrZero Integer overdueDays,
        @NotNull @PositiveOrZero Double totalExposure,

        String phoneNumber,
        String address,

        // Optional — falls back to a synthetic demo address if unset, never required
        String customerEmail,
        String customerName
) {}
