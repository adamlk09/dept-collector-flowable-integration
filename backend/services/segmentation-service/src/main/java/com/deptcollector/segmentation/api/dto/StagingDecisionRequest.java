package com.deptcollector.segmentation.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Input for the banking-recovery staging chain (collectionStagingDecision -> ...) — ageing,
 * exposure, and (Milestone 2, optional) customerType for customerSegmentationDecision.
 */
public record StagingDecisionRequest(
        @NotBlank String debtId,
        @NotNull Integer overdueDays,
        @NotNull Double totalExposure,

        // PARTICULIER | PROFESSIONNEL | ENTREPRISE — optional, defaults to PARTICULIER
        String customerType
) {}
