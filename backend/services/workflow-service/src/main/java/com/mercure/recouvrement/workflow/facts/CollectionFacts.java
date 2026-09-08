package com.mercure.recouvrement.workflow.facts;

/**
 * The categorical facts (EPIC 1–6 results + reachability) used to qualify a debt file.
 * Produced by {@link CollectionFactsResolver}; consumed by the qualification step.
 */
public record CollectionFacts(
        String clientInfoStatus,   // EPIC 1
        String contractStatus,     // EPIC 2
        String debtStatus,         // EPIC 3
        String paymentHistory,     // EPIC 4
        String collectionStage,    // EPIC 5
        String promiseStatus,      // EPIC 6
        boolean reachable
) {}
