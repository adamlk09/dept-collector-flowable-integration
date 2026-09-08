package com.deptcollector.notification.api.dto;

import com.deptcollector.notification.domain.EmailDelivery;

import java.time.Instant;
import java.util.UUID;

public record EmailDeliveryResponse(
        UUID id,
        String recipient,
        String subject,
        String templateName,
        String triggerType,
        String status,
        String errorMessage,
        Instant sentAt
) {
    public static EmailDeliveryResponse from(EmailDelivery e) {
        return new EmailDeliveryResponse(e.getId(), e.getRecipient(), e.getSubject(), e.getTemplateName(),
                e.getTriggerType(), e.getStatus(), e.getErrorMessage(), e.getSentAt());
    }
}
