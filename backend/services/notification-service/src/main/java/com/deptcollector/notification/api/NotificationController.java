package com.deptcollector.notification.api;

import com.deptcollector.notification.api.dto.EmailDeliveryResponse;
import com.deptcollector.notification.api.dto.SendEmailRequest;
import com.deptcollector.notification.domain.EmailDelivery;
import com.deptcollector.notification.domain.EmailDeliveryRepository;
import com.deptcollector.notification.domain.EmailService;
import com.deptcollector.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final EmailService emailService;
    private final EmailDeliveryRepository deliveryRepository;

    public NotificationController(EmailService emailService, EmailDeliveryRepository deliveryRepository) {
        this.emailService = emailService;
        this.deliveryRepository = deliveryRepository;
    }

    @PostMapping("/email")
    public EmailDeliveryResponse sendEmail(@Valid @RequestBody SendEmailRequest request) {
        EmailDelivery delivery = emailService.send(request);
        return EmailDeliveryResponse.from(delivery);
    }

    @GetMapping("/email")
    public List<EmailDeliveryResponse> listDeliveries() {
        String tenantId = TenantContext.getRequiredTenantId();
        return deliveryRepository.findByTenantIdOrderBySentAtDesc(tenantId).stream()
                .map(EmailDeliveryResponse::from)
                .toList();
    }
}
