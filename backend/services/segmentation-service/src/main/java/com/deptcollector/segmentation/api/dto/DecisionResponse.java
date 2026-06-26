package com.deptcollector.segmentation.api.dto;

public record DecisionResponse(
        String debtId,
        String segment,
        String explication,
        boolean simulated
) {}
