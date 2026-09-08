package com.mercure.recouvrement.workflow.facts;

/**
 * EPIC 3 — resolves the debt status
 * ({@code OPEN_DEBT} / {@code PAID_DEBT} / {@code PARTIAL_PAYMENT}).
 *
 * <p>Default implementation echoes the value supplied with the request. A real
 * implementation will call debt-service once it exposes an API.
 */
@FunctionalInterface
public interface DebtStatusProvider {

    String resolve(DebtFileContext context, String provided);
}
