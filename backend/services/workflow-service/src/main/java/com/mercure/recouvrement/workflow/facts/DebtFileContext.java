package com.mercure.recouvrement.workflow.facts;

/**
 * Identifiers a fact provider needs to look up a debt file in the business services
 * (debt-service, customer-service, …) once those expose APIs. Carries the tenant and
 * correlation so providers can propagate them on outbound calls.
 */
public record DebtFileContext(
        String debtId,
        String customerId,
        String tenantId,
        String correlationId
) {}
