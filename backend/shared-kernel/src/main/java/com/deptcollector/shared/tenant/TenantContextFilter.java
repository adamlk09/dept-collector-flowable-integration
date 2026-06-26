package com.deptcollector.shared.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantContextFilter extends OncePerRequestFilter {
    public static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (isTenantExempt(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        var tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId == null || tenantId.isBlank()) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Missing X-Tenant-Id header");
            return;
        }

        try {
            TenantContext.set(tenantId.trim());
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Infrastructure and API-documentation endpoints are not tenant-scoped, so they must
     * load without an {@code X-Tenant-Id} header (a browser opening Swagger UI sends none).
     */
    private boolean isTenantExempt(String uri) {
        return uri.startsWith("/actuator")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs");
    }
}
