package com.deptcollector.shared.web;

import com.deptcollector.shared.tenant.TenantContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/_service")
public class ServiceInfoController {
    private final String serviceName;
    private final String boundedContext;
    private final List<String> capabilities;

    public ServiceInfoController(
            @Value("${spring.application.name}") String serviceName,
            @Value("${app.bounded-context}") String boundedContext,
            @Value("${app.capabilities:}") List<String> capabilities) {
        this.serviceName = serviceName;
        this.boundedContext = boundedContext;
        this.capabilities = capabilities;
    }

    @GetMapping
    public Map<String, Object> info() {
        return Map.of(
                "service", serviceName,
                "boundedContext", boundedContext,
                "tenantId", TenantContext.getRequiredTenantId(),
                "capabilities", capabilities,
                "status", "SKELETON_READY",
                "timestamp", Instant.now());
    }
}
