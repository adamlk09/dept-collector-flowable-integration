package com.deptcollector.shared.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DomainEvent(
        UUID eventId,
        String eventType,
        String aggregateId,
        String tenantId,
        Instant occurredAt,
        Map<String, Object> payload) {

    public static DomainEvent of(
            String eventType,
            String aggregateId,
            String tenantId,
            Map<String, Object> payload) {
        return new DomainEvent(UUID.randomUUID(), eventType, aggregateId, tenantId, Instant.now(), payload);
    }
}
