package com.deptcollector.segmentation.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record DecisionRequest(
        @NotBlank  String debtId,
        @NotNull @PositiveOrZero Double amount,
        @NotNull @PositiveOrZero Integer daysOverdue,
        @NotBlank  String clientType
) {}
