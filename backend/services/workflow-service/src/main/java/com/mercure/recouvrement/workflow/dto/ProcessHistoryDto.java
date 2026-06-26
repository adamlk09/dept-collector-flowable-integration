package com.mercure.recouvrement.workflow.dto;

import java.util.Date;

public record ProcessHistoryDto(
        String activityId,
        String activityName,
        String activityType,
        Date startTime,
        Date endTime,
        Long durationMillis
) {}
