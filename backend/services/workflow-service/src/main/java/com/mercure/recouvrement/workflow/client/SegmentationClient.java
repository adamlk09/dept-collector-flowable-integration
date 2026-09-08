package com.mercure.recouvrement.workflow.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin REST client to segmentation-service's collection-qualification DMN.
 *
 * <p>Propagates the mandatory {@code X-Tenant-Id} and (when present) the {@code X-Correlation-Id}
 * so the downstream service runs under the same tenant and correlation context. Callers are
 * expected to handle failures (the BPMN delegate falls back to its local engine).
 */
@Component
public class SegmentationClient {

    private final RestClient restClient;

    public SegmentationClient(RestClient.Builder builder,
                              @Value("${app.segmentation-service.url:http://localhost:8092}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public SegmentationDecisionResponse qualify(String tenantId, String correlationId,
                                                SegmentationDecisionRequest request) {
        return restClient.post()
                .uri("/api/v1/segmentation/execute")
                .headers(headers -> {
                    headers.set("X-Tenant-Id", tenantId);
                    if (correlationId != null && !correlationId.isBlank()) {
                        headers.set("X-Correlation-Id", correlationId);
                    }
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(SegmentationDecisionResponse.class);
    }

    /**
     * Second, independent decision: banking-recovery staging (ageing + exposure only, no
     * client-qualification fields). Same tenant/correlation propagation as {@link #qualify}.
     */
    public StagingDecisionResponse stageRecovery(String tenantId, String correlationId,
                                                 StagingDecisionRequest request) {
        return restClient.post()
                .uri("/api/v1/segmentation/staging/execute")
                .headers(headers -> {
                    headers.set("X-Tenant-Id", tenantId);
                    if (correlationId != null && !correlationId.isBlank()) {
                        headers.set("X-Correlation-Id", correlationId);
                    }
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(StagingDecisionResponse.class);
    }
}
