package com.mercure.recouvrement.workflow.dto;

import java.util.Date;

public record JobDto(
        String jobId,
        String processInstanceId,
        String activityId,
        String activityName,
        Date dueDate,
        int retries,
        String exceptionMessage
) {}
