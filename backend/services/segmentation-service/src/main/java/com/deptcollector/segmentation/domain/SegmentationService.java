package com.deptcollector.segmentation.domain;

import com.deptcollector.segmentation.api.dto.DecisionRequest;
import com.deptcollector.segmentation.api.dto.DecisionResponse;
import com.deptcollector.shared.tenant.TenantContext;
import org.flowable.dmn.api.DmnRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SegmentationService {

    private static final Logger log = LoggerFactory.getLogger(SegmentationService.class);
    private static final String DECISION_KEY = "debtSegmentation";

    private final DmnRuleService dmnRuleService;

    public SegmentationService(DmnRuleService dmnRuleService) {
        this.dmnRuleService = dmnRuleService;
    }

    public DecisionResponse execute(DecisionRequest request) {
        return evaluate(request, false);
    }

    public DecisionResponse simulate(DecisionRequest request) {
        return evaluate(request, true);
    }

    private DecisionResponse evaluate(DecisionRequest request, boolean simulated) {
        String tenantId = TenantContext.getRequiredTenantId();

        log.info("DMN evaluate debtId={} amount={} daysOverdue={} clientType={} tenantId={} simulated={}",
                request.debtId(), request.amount(), request.daysOverdue(), request.clientType(), tenantId, simulated);

        Map<String, Object> result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey(DECISION_KEY)
                .tenantId(tenantId)
                .variable("amount", request.amount())
                .variable("daysOverdue", request.daysOverdue())
                .variable("clientType", request.clientType())
                .executeWithSingleResult();

        if (result == null || result.isEmpty()) {
            log.warn("DMN returned no result for debtId={} — falling back to STANDARD", request.debtId());
            return new DecisionResponse(request.debtId(), "STANDARD", "R6 — Cas standard : aucune règle déclenchée", simulated);
        }

        String segment = (String) result.get("segment");
        String explication = (String) result.get("explication");

        log.info("DMN result debtId={} segment={} explication={}", request.debtId(), segment, explication);
        return new DecisionResponse(request.debtId(), segment, explication, simulated);
    }
}
