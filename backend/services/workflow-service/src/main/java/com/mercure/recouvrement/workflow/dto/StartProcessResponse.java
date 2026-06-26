package com.mercure.recouvrement.workflow.dto;

public record StartProcessResponse(
        String processInstanceId,
        String businessKey,
        boolean created
) {}
