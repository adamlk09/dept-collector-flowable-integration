package com.deptcollector.casefile.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCaseStatusRequest(
        @NotBlank String status,
        String assignedCollector
) {}
