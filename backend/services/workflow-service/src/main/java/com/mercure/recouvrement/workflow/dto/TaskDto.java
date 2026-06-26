package com.mercure.recouvrement.workflow.dto;

import java.util.Date;

public record TaskDto(
        String taskId,
        String name,
        String processInstanceId,
        String processDefinitionId,
        Date createTime
) {}
