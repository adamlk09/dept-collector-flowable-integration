package com.deptcollector.segmentation.domain;

import com.deptcollector.segmentation.api.dto.DecisionRequest;
import com.deptcollector.segmentation.api.dto.DecisionResponse;
import com.deptcollector.shared.tenant.TenantContext;
import org.flowable.dmn.api.DmnDecisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SegmentationService {

    private static final Logger log = LoggerFactory.getLogger(SegmentationService.class);
    private static final String DECISION_KEY = "collectionQualification";

    private final DmnDecisionService dmnDecisionService;

    public SegmentationService(DmnDecisionService dmnDecisionService) {
        this.dmnDecisionService = dmnDecisionService;
    }

    public DecisionResponse execute(DecisionRequest request) {
        return evaluate(request, false);
    }

    public DecisionResponse simulate(DecisionRequest request) {
        return evaluate(request, true);
    }

    private DecisionResponse evaluate(DecisionRequest request, boolean simulated) {
        String tenantId = TenantContext.getRequiredTenantId();

        log.info("DMN evaluate debtId={} clientInfo={} contract={} debt={} history={} stage={} promise={} reachable={} tenantId={} simulated={}",
                request.debtId(), request.clientInfoStatus(), request.contractStatus(), request.debtStatus(),
                request.paymentHistory(), request.collectionStage(), request.promiseStatus(), request.reachable(),
                tenantId, simulated);

        // The decision table auto-deploys to the default (tenantless) deployment, so resolution
        // stays tenantless too — scoping it to tenantId would make Flowable look for a decision
        // "for tenant X", which was never deployed, and throw FlowableObjectNotFoundException.
        Map<String, Object> result = dmnDecisionService.createExecuteDecisionBuilder()
                .decisionKey(DECISION_KEY)
                .variable("clientInfoStatus", request.clientInfoStatus())
                .variable("contractStatus", request.contractStatus())
                .variable("debtStatus", request.debtStatus())
                .variable("paymentHistory", request.paymentHistory())
                .variable("collectionStage", request.collectionStage())
                .variable("promiseStatus", request.promiseStatus())
                .variable("reachable", request.reachable() == null ? Boolean.TRUE : request.reachable())
                .executeWithSingleResult();

        if (result == null || result.isEmpty()) {
            log.warn("DMN returned no result for debtId={} — falling back to FIRST_CONTACT", request.debtId());
            return new DecisionResponse(request.debtId(), "FIRST_CONTACT",
                    "First contact; no rule matched.", simulated);
        }

        String qualification = (String) result.get("qualification");
        String qualificationReason = (String) result.get("qualificationReason");

        log.info("DMN result debtId={} qualification={} reason={}", request.debtId(), qualification, qualificationReason);
        return new DecisionResponse(request.debtId(), qualification, qualificationReason, simulated);
    }
}
