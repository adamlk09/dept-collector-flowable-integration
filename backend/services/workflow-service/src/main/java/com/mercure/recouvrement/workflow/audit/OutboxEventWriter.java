package com.mercure.recouvrement.workflow.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * doc §26: every important action must be traceable, not just via application logs. Writes into
 * the existing (previously unused) {@code outbox_event} table from shared-kernel's baseline
 * migration — the Outbox pattern already established for this project, not a bespoke audit call
 * chain. A downstream audit-service consumer is out of scope for this pass; this writer alone
 * already makes every case-status/task-action change queryable and durable.
 */
@Component
public class OutboxEventWriter {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventWriter.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public OutboxEventWriter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void write(String tenantId, String aggregateType, String aggregateId, String eventType,
                      Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            jdbcTemplate.update(
                    "INSERT INTO outbox_event (id, tenant_id, aggregate_type, aggregate_id, event_type, payload, occurred_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?)",
                    UUID.randomUUID(), tenantId, aggregateType, aggregateId, eventType, json,
                    Timestamp.from(Instant.now()));
        } catch (Exception e) {
            // Audit is best-effort observability, never a reason to fail the business operation.
            log.warn("Failed to write audit event type={} aggregateId={}: {}", eventType, aggregateId, e.getMessage());
        }
    }
}
