package com.mercure.recouvrement.workflow.facts;

/**
 * EPIC 2 — resolves the contract status
 * ({@code ACTIVE_CONTRACT} / {@code SUSPENDED_CONTRACT} / {@code TERMINATED_CONTRACT}).
 *
 * <p>Default implementation echoes the value supplied with the request. A real
 * implementation will call contract-service once it exposes an API.
 */
@FunctionalInterface
public interface ContractStatusProvider {

    String resolve(DebtFileContext context, String provided);
}
