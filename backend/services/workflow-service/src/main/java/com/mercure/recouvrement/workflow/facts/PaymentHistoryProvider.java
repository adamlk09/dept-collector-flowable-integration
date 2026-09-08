package com.mercure.recouvrement.workflow.facts;

/**
 * EPIC 4 — resolves the payment history
 * ({@code GOOD_PAYMENT_HISTORY} / {@code IRREGULAR_PAYMENT_HISTORY} / {@code NO_PAYMENT}).
 *
 * <p>Default implementation echoes the value supplied with the request. A real
 * implementation will call payment-service once it exposes an API.
 */
@FunctionalInterface
public interface PaymentHistoryProvider {

    String resolve(DebtFileContext context, String provided);
}
