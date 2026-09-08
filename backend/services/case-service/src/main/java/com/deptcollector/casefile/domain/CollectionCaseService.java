package com.deptcollector.casefile.domain;

import com.deptcollector.casefile.api.dto.CreateCaseRequest;
import com.deptcollector.shared.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class CollectionCaseService {

    private static final Logger log = LoggerFactory.getLogger(CollectionCaseService.class);

    private final CollectionCaseRepository repository;

    public CollectionCaseService(CollectionCaseRepository repository) {
        this.repository = repository;
    }

    /**
     * Idempotent by (tenantId, caseReference) — a retry or a re-ingested batch row returns the
     * existing case instead of creating a duplicate, same pattern as workflow-service's process start.
     */
    @Transactional
    public CollectionCase createOrGet(CreateCaseRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();

        return repository.findByTenantIdAndCaseReference(tenantId, request.caseReference())
                .orElseGet(() -> {
                    CollectionCase created = new CollectionCase(UUID.randomUUID(), tenantId,
                            request.caseReference(), request.customerId(), request.customerType(),
                            request.daysOverdue(), request.globalExposure(), request.ifrsStage(),
                            request.collectionSegment(), request.priority(), request.processInstanceId(),
                            Instant.now());
                    CollectionCase saved = repository.save(created);
                    log.info("Case created caseReference={} tenantId={}", request.caseReference(), tenantId);
                    return saved;
                });
    }

    public CollectionCase get(UUID id) {
        TenantContext.getRequiredTenantId();
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Case not found: " + id));
    }

    public List<CollectionCase> list() {
        String tenantId = TenantContext.getRequiredTenantId();
        return repository.findByTenantId(tenantId);
    }

    @Transactional
    public CollectionCase updateStatus(UUID id, String status, String assignedCollector) {
        TenantContext.getRequiredTenantId();
        CollectionCase c = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Case not found: " + id));
        c.updateStatus(status, assignedCollector, Instant.now());
        log.info("Case status updated id={} status={}", id, status);
        return c;
    }
}
