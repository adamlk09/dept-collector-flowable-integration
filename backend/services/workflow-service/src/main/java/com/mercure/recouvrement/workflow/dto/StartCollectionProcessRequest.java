package com.mercure.recouvrement.workflow.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Behavioral input for the collection qualification engine (see doc/segmentation.md).
 *
 * <p>There is no numeric score: the file is qualified from the categorical results of
 * EPIC 1–6. Every categorical field is optional — the engine applies a safe default when
 * a dimension is not provided — so callers that only know the debt and customer still work.
 */
public record StartCollectionProcessRequest(
        @NotBlank String debtId,
        @NotBlank String customerId,

        // EPIC 1 — client information completeness:
        //   CLIENT_INFORMATION_COMPLETE | CLIENT_INFORMATION_INCOMPLETE
        String clientInfoStatus,

        // EPIC 2 — contract status:
        //   ACTIVE_CONTRACT | SUSPENDED_CONTRACT | TERMINATED_CONTRACT
        String contractStatus,

        // EPIC 3 — debt status:
        //   OPEN_DEBT | PAID_DEBT | PARTIAL_PAYMENT
        String debtStatus,

        // EPIC 4 — payment history:
        //   GOOD_PAYMENT_HISTORY | IRREGULAR_PAYMENT_HISTORY | NO_PAYMENT
        String paymentHistory,

        // EPIC 5 — collection-action history:
        //   FIRST_CONTACT | FOLLOW_UP | MULTIPLE_FOLLOW_UPS
        String collectionStage,

        // EPIC 6 — payment promise:
        //   NONE | PROMISE_ACTIVE | PROMISE_KEPT | PROMISE_BROKEN
        String promiseStatus,

        // Cas 6 — whether the client can currently be reached
        Boolean reachable
) {}
