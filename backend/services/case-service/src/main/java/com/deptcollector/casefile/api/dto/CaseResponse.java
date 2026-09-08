package com.deptcollector.casefile.api.dto;

import com.deptcollector.casefile.domain.CollectionCase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CaseResponse(
        UUID id,
        String caseReference,
        String customerId,
        String customerType,
        Integer daysOverdue,
        BigDecimal globalExposure,
        String ifrsStage,
        String collectionSegment,
        String priority,
        String status,
        String assignedCollector,
        String processInstanceId,
        Instant createdAt,
        Instant updatedAt
) {
    public static CaseResponse from(CollectionCase c) {
        return new CaseResponse(c.getId(), c.getCaseReference(), c.getCustomerId(), c.getCustomerType(),
                c.getDaysOverdue(), c.getGlobalExposure(), c.getIfrsStage(), c.getCollectionSegment(),
                c.getPriority(), c.getStatus(), c.getAssignedCollector(), c.getProcessInstanceId(),
                c.getCreatedAt(), c.getUpdatedAt());
    }
}
