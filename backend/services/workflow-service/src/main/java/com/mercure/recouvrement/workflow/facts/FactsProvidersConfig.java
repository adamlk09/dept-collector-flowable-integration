package com.mercure.recouvrement.workflow.facts;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default fact providers (M4 anticorruption layer).
 *
 * <p>Each provider echoes the value supplied with the request, applying a safe categorical
 * default when none was provided — i.e. current behavior is preserved. When a business service
 * (customer/contract/debt/payment) starts exposing an API, replace the corresponding provider
 * with a real implementation; {@link ConditionalOnMissingBean} lets a real bean take over
 * without touching this class.
 */
@Configuration
public class FactsProvidersConfig {

    @Bean
    @ConditionalOnMissingBean
    public ClientInfoProvider clientInfoProvider() {
        return (context, provided) -> orDefault(provided, "CLIENT_INFORMATION_COMPLETE");
    }

    @Bean
    @ConditionalOnMissingBean
    public ContractStatusProvider contractStatusProvider() {
        return (context, provided) -> orDefault(provided, "ACTIVE_CONTRACT");
    }

    @Bean
    @ConditionalOnMissingBean
    public DebtStatusProvider debtStatusProvider() {
        return (context, provided) -> orDefault(provided, "OPEN_DEBT");
    }

    @Bean
    @ConditionalOnMissingBean
    public PaymentHistoryProvider paymentHistoryProvider() {
        return (context, provided) -> orDefault(provided, "GOOD_PAYMENT_HISTORY");
    }

    @Bean
    @ConditionalOnMissingBean
    public CollectionStageProvider collectionStageProvider() {
        return (context, provided) -> orDefault(provided, "FIRST_CONTACT");
    }

    @Bean
    @ConditionalOnMissingBean
    public PromiseStatusProvider promiseStatusProvider() {
        return (context, provided) -> orDefault(provided, "NONE");
    }

    private static String orDefault(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
