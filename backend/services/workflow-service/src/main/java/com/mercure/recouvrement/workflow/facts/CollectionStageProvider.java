package com.mercure.recouvrement.workflow.facts;

/**
 * EPIC 5 — resolves the collection-action stage
 * ({@code FIRST_CONTACT} / {@code FOLLOW_UP} / {@code MULTIPLE_FOLLOW_UPS}).
 *
 * <p>Default implementation echoes the value supplied with the request. A real
 * implementation will call case-service / the collection-action history once it exposes an API.
 */
@FunctionalInterface
public interface CollectionStageProvider {

    String resolve(DebtFileContext context, String provided);
}
