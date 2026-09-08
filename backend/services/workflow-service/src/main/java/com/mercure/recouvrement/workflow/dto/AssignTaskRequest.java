package com.mercure.recouvrement.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignTaskRequest(
        @NotBlank String assignee
) {}
