package com.mercure.recouvrement.workflow.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/**
 * Best-effort client to case-service's business-data store (doc §30's separation: Flowable owns
 * runtime/history, case-service owns the durable case snapshot). Never throws — a case-service
 * outage must not block the BPMN staging step; callers just don't get a caseId back.
 */
@Component
public class CaseServiceClient {

    private static final Logger log = LoggerFactory.getLogger(CaseServiceClient.class);

    private final RestClient restClient;

    public CaseServiceClient(RestClient.Builder builder,
                             @Value("${app.case-service.url:http://localhost:8091}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public void createOrGetCase(String tenantId, String caseReference, String customerId, String customerType,
                                Integer daysOverdue, BigDecimal globalExposure, String ifrsStage,
                                String collectionSegment, String priority, String processInstanceId) {
        try {
            restClient.post()
                    .uri("/api/v1/cases")
                    .header("X-Tenant-Id", tenantId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CreateCaseRequest(caseReference, customerId, customerType, daysOverdue,
                            globalExposure, ifrsStage, collectionSegment, priority, processInstanceId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("case-service unavailable for caseReference={} — case snapshot not persisted: {}",
                    caseReference, e.getMessage());
        }
    }

    public record CreateCaseRequest(
            String caseReference, String customerId, String customerType, Integer daysOverdue,
            BigDecimal globalExposure, String ifrsStage, String collectionSegment, String priority,
            String processInstanceId
    ) {}
}
