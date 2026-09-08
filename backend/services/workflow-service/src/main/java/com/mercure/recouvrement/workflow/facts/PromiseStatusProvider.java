package com.mercure.recouvrement.workflow.facts;

/**
 * EPIC 6 — resolves the payment-promise status
 * ({@code NONE} / {@code PROMISE_ACTIVE} / {@code PROMISE_KEPT} / {@code PROMISE_BROKEN}).
 *
 * <p>Default implementation echoes the value supplied with the request. A real
 * implementation will call the promise/payment service once it exposes an API.
 */
@FunctionalInterface
public interface PromiseStatusProvider {

    String resolve(DebtFileContext context, String provided);
}
