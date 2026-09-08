package com.deptcollector.notification.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * templateName selects one of the classpath templates/*.html files (collection-reminder,
 * high-exposure-alert, formal-notice, escalation, payment-confirmation). variables are
 * substituted for {{placeholder}} tokens in the template body — doc §18/§19: templates,
 * not hard-coded HTML in Java.
 */
public record SendEmailRequest(
        @NotBlank String to,
        @NotBlank String subject,
        @NotBlank String templateName,
        Map<String, String> variables,

        // AUTOMATIC | AGENT_TRIGGERED — doc §20's distinction, recorded for audit
        String triggerType
) {}
