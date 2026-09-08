package com.mercure.recouvrement.workflow.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Best-effort client to notification-service — doc §18/§19/§20: automatic emails (from BPMN
 * service tasks) and agent-triggered emails (from business-action endpoints) both go through
 * here, never straight from a controller. A notification-service outage never blocks the
 * workflow — failures are logged and swallowed, matching notification-service's own
 * "record FAILED, never throw" contract for a bad SMTP connection.
 */
@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient restClient;

    public NotificationClient(RestClient.Builder builder,
                              @Value("${app.notification-service.url:http://localhost:8095}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public void sendEmail(String tenantId, String to, String subject, String templateName,
                          Map<String, String> variables, String triggerType) {
        try {
            restClient.post()
                    .uri("/api/v1/notifications/email")
                    .header("X-Tenant-Id", tenantId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SendEmailRequest(to, subject, templateName, variables, triggerType))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("notification-service unavailable, email not sent to={} template={}: {}",
                    to, templateName, e.getMessage());
        }
    }

    public record SendEmailRequest(String to, String subject, String templateName,
                                   Map<String, String> variables, String triggerType) {}
}
