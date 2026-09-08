package com.mercure.recouvrement.workflow.facts;

import org.springframework.stereotype.Component;

/**
 * Aggregates the per-EPIC providers into a single {@link CollectionFacts}.
 *
 * <p>This is the seam M4 introduces: the qualification step no longer reads dimensions straight
 * from the request — it asks the providers to resolve them. Today every provider echoes the
 * provided input (stub), so behavior is unchanged; tomorrow each can call its business service.
 */
@Component
public class CollectionFactsResolver {

    private final ClientInfoProvider clientInfoProvider;
    private final ContractStatusProvider contractStatusProvider;
    private final DebtStatusProvider debtStatusProvider;
    private final PaymentHistoryProvider paymentHistoryProvider;
    private final CollectionStageProvider collectionStageProvider;
    private final PromiseStatusProvider promiseStatusProvider;

    public CollectionFactsResolver(ClientInfoProvider clientInfoProvider,
                                   ContractStatusProvider contractStatusProvider,
                                   DebtStatusProvider debtStatusProvider,
                                   PaymentHistoryProvider paymentHistoryProvider,
                                   CollectionStageProvider collectionStageProvider,
                                   PromiseStatusProvider promiseStatusProvider) {
        this.clientInfoProvider = clientInfoProvider;
        this.contractStatusProvider = contractStatusProvider;
        this.debtStatusProvider = debtStatusProvider;
        this.paymentHistoryProvider = paymentHistoryProvider;
        this.collectionStageProvider = collectionStageProvider;
        this.promiseStatusProvider = promiseStatusProvider;
    }

    /**
     * Resolves the categorical facts for the file. {@code provided} carries the values supplied
     * with the request (may contain nulls); reachability is passed through unchanged.
     */
    public CollectionFacts resolve(DebtFileContext context, CollectionFacts provided) {
        return new CollectionFacts(
                clientInfoProvider.resolve(context, provided.clientInfoStatus()),
                contractStatusProvider.resolve(context, provided.contractStatus()),
                debtStatusProvider.resolve(context, provided.debtStatus()),
                paymentHistoryProvider.resolve(context, provided.paymentHistory()),
                collectionStageProvider.resolve(context, provided.collectionStage()),
                promiseStatusProvider.resolve(context, provided.promiseStatus()),
                provided.reachable());
    }
}
