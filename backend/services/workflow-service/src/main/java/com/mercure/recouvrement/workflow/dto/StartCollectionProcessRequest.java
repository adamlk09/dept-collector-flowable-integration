package com.mercure.recouvrement.workflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StartCollectionProcessRequest(
        @NotBlank String debtId,
        @NotBlank String customerId,
        @NotNull @Min(0) @Max(100) Integer score
) {}
