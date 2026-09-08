package com.deptcollector.casefile.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateCaseRequest(
        @NotBlank String caseReference,
        @NotBlank String customerId,
        String customerType,
        Integer daysOverdue,
        BigDecimal globalExposure,
        String ifrsStage,
        String collectionSegment,
        String priority,
        String processInstanceId
) {}
