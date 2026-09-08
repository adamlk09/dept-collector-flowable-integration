package com.deptcollector.segmentation.api.dto;

public record DecisionResponse(
        String debtId,
        String qualification,
        String qualificationReason,
        boolean simulated
) {}
