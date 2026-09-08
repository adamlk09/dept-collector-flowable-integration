package com.deptcollector.notification.domain;

import com.deptcollector.notification.api.dto.SendEmailRequest;
import com.deptcollector.shared.tenant.TenantContext;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * doc §18/§19: the process reaches SMTP only through this service, never from a controller
 * directly. Templates live as classpath HTML files, not hard-coded strings; a send failure
 * (unreachable SMTP, bad template) is recorded and returned as a normal response — it never
 * throws back into the caller, so a workflow step that emails never gets blocked by mail outages.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EmailDeliveryRepository repository;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender, EmailDeliveryRepository repository,
                        @Value("${app.notification.from-address:collections@deptcollector.local}") String fromAddress) {
        this.mailSender = mailSender;
        this.repository = repository;
        this.fromAddress = fromAddress;
    }

    public EmailDelivery send(SendEmailRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();
        String triggerType = request.triggerType() != null ? request.triggerType() : "AUTOMATIC";

        String status;
        String errorMessage = null;
        try {
            String body = renderTemplate(request.templateName(), request.variables());
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(request.to());
            helper.setSubject(request.subject());
            helper.setText(body, true);
            mailSender.send(message);
            status = "SENT";
            log.info("Email sent to={} template={} trigger={} tenantId={}",
                    request.to(), request.templateName(), triggerType, tenantId);
        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            log.warn("Email send failed to={} template={} trigger={} tenantId={}: {}",
                    request.to(), request.templateName(), triggerType, tenantId, e.getMessage());
        }

        EmailDelivery delivery = new EmailDelivery(UUID.randomUUID(), tenantId, request.to(),
                request.subject(), request.templateName(), triggerType, status, errorMessage, Instant.now());
        return repository.save(delivery);
    }

    private String renderTemplate(String templateName, Map<String, String> variables) throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/" + templateName + ".html");
        String template;
        try (InputStream in = resource.getInputStream()) {
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                template = template.replace("{{" + entry.getKey() + "}}",
                        entry.getValue() != null ? entry.getValue() : "");
            }
        }
        return template;
    }
}
