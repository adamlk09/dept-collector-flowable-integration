package com.mercure.recouvrement.workflow.facts;

/**
 * EPIC 1 — resolves the client-information completeness
 * ({@code CLIENT_INFORMATION_COMPLETE} / {@code CLIENT_INFORMATION_INCOMPLETE}).
 *
 * <p>Default implementation echoes the value supplied with the request. A real
 * implementation will call customer-service once it exposes an API.
 */
@FunctionalInterface
public interface ClientInfoProvider {

    String resolve(DebtFileContext context, String provided);
}
