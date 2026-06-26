package com.mercure.recouvrement.workflow.dto;

import java.util.Date;

public record IncidentDto(
        String incidentId,
        String processInstanceId,
        String activityId,
        String type,
        String message,
        Date createTime
) {}
