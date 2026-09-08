package com.mercure.recouvrement.workflow.delegate;

import com.mercure.recouvrement.workflow.client.SegmentationClient;
import com.mercure.recouvrement.workflow.client.SegmentationDecisionRequest;
import com.mercure.recouvrement.workflow.client.SegmentationDecisionResponse;
import com.mercure.recouvrement.workflow.facts.CollectionFacts;
import com.mercure.recouvrement.workflow.facts.CollectionFactsResolver;
import com.mercure.recouvrement.workflow.facts.DebtFileContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Collection qualification step (see doc/segmentation.md, EPIC 7).
 *
 * <p>Pipeline: <b>resolve facts</b> (M4 — per-EPIC providers, stubbed to echo the request)
 * → <b>qualify</b> (M3 — segmentation-service DMN, with a local fallback). The rules carry no
 * numeric scoring. The DMN call propagates tenant/correlation headers; if it fails for any
 * reason the local engine keeps the process moving.
 *
 * <p>Sets {@code qualification}, {@code qualificationReason}, the derived {@code segment}
 * ({@code HIGH_RISK}/{@code LOW_RISK}) the gateway routes on, the resolved fact variables, and
 * {@code qualificationSource} ({@code DMN}/{@code LOCAL}) for observability.
 */
@Component("segmentDebtDelegate")
public class SegmentDebtDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(SegmentDebtDelegate.class);

    /** Qualifications that map to the high-risk (priority) path. */
    private static final Set<String> HIGH_RISK_QUALIFICATIONS = Set.of("LEGAL", "PRE_LEGAL", "PROMISE_BROKEN");

    private final SegmentationClient segmentationClient;
    private final CollectionFactsResolver factsResolver;

    public SegmentDebtDelegate(SegmentationClient segmentationClient,
                               CollectionFactsResolver factsResolver) {
        this.segmentationClient = segmentationClient;
        this.factsResolver = factsResolver;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String debtId = asString(execution.getVariable("debtId"), null);

        // M4 — resolve the EPIC 1–6 facts through the provider layer (stubbed to echo the request).
        DebtFileContext context = new DebtFileContext(
                debtId,
                asString(execution.getVariable("customerId"), null),
                asString(execution.getVariable("tenantId"), null),
                MDC.get("correlationId"));
        CollectionFacts provided = new CollectionFacts(
                asString(execution.getVariable("clientInfoStatus"), null),
                asString(execution.getVariable("contractStatus"), null),
                asString(execution.getVariable("debtStatus"), null),
                asString(execution.getVariable("paymentHistory"), null),
                asString(execution.getVariable("collectionStage"), null),
                asString(execution.getVariable("promiseStatus"), null),
                asBool(execution.getVariable("reachable"), true));
        CollectionFacts facts = factsResolver.resolve(context, provided);

        // M3 — qualify the resolved facts via the DMN, falling back to the local engine.
        Qualification qualification;
        String source;
        try {
            SegmentationDecisionResponse response = segmentationClient.qualify(
                    context.tenantId(), context.correlationId(),
                    new SegmentationDecisionRequest(debtId, facts.clientInfoStatus(), facts.contractStatus(),
                            facts.debtStatus(), facts.paymentHistory(), facts.collectionStage(),
                            facts.promiseStatus(), facts.reachable()));
            if (response == null || response.qualification() == null) {
                throw new IllegalStateException("segmentation-service returned an empty qualification");
            }
            qualification = new Qualification(response.qualification(), response.qualificationReason());
            source = "DMN";
        } catch (Exception e) {
            log.warn("DMN qualification unavailable for pid={} debtId={} — falling back to local engine: {}",
                    processInstanceId, debtId, e.getMessage());
            qualification = qualifyLocally(facts);
            source = "LOCAL";
        }

        String segment = HIGH_RISK_QUALIFICATIONS.contains(qualification.code) ? "HIGH_RISK" : "LOW_RISK";

        // Persist the resolved facts so history reflects what was actually qualified.
        execution.setVariable("clientInfoStatus", facts.clientInfoStatus());
        execution.setVariable("contractStatus", facts.contractStatus());
        execution.setVariable("debtStatus", facts.debtStatus());
        execution.setVariable("paymentHistory", facts.paymentHistory());
        execution.setVariable("collectionStage", facts.collectionStage());
        execution.setVariable("promiseStatus", facts.promiseStatus());
        execution.setVariable("reachable", facts.reachable());

        execution.setVariable("qualification", qualification.code);
        execution.setVariable("qualificationReason", qualification.reason);
        execution.setVariable("qualificationSource", source);
        execution.setVariable("segment", segment);
        execution.setVariable("segmentedAt", System.currentTimeMillis());

        log.info("Debt qualified pid={} source={} qualification={} segment={} reason='{}'",
                processInstanceId, source, qualification.code, segment, qualification.reason);
    }

    /**
     * Local fallback mirroring the DMN: evaluates the EPIC 7 cases in priority order; first match wins.
     */
    private Qualification qualifyLocally(CollectionFacts facts) {
        // Case 1 — incomplete client information blocks everything else.
        if ("CLIENT_INFORMATION_INCOMPLETE".equalsIgnoreCase(facts.clientInfoStatus())) {
            return new Qualification("CLIENT_INFORMATION_INCOMPLETE",
                    "Client information is incomplete; collection cannot start.");
        }
        // Case 2 — debt already settled: nothing to collect.
        if ("PAID_DEBT".equalsIgnoreCase(facts.debtStatus())) {
            return new Qualification("DEBT_CLOSED",
                    "Debt is already paid; file should be closed.");
        }
        // Case 6 — client unreachable: an information-research mission is required first.
        if (!facts.reachable()) {
            return new Qualification("INFO_RESEARCH",
                    "Client is unreachable; an information-research mission is required.");
        }
        // Case 8 — legal conditions met: terminated contract, repeated unsuccessful follow-ups, no payment.
        if ("TERMINATED_CONTRACT".equalsIgnoreCase(facts.contractStatus())
                && "MULTIPLE_FOLLOW_UPS".equalsIgnoreCase(facts.collectionStage())
                && "NO_PAYMENT".equalsIgnoreCase(facts.paymentHistory())) {
            return new Qualification("LEGAL",
                    "Legal conditions met: terminated contract, multiple unsuccessful follow-ups, no payment.");
        }
        // Case 7 — significant overdue with multiple unsuccessful follow-ups: transfer to pre-legal.
        if ("MULTIPLE_FOLLOW_UPS".equalsIgnoreCase(facts.collectionStage())) {
            return new Qualification("PRE_LEGAL",
                    "Multiple unsuccessful follow-ups; transfer to the pre-legal service.");
        }
        // Case 5 — broken payment promise: escalate.
        if ("PROMISE_BROKEN".equalsIgnoreCase(facts.promiseStatus())) {
            return new Qualification("PROMISE_BROKEN",
                    "A payment promise was broken; escalate the file.");
        }
        // Case 4 — already contacted once: schedule a follow-up.
        if ("FOLLOW_UP".equalsIgnoreCase(facts.collectionStage())) {
            return new Qualification("FOLLOW_UP",
                    "A prior contact was made; schedule a follow-up.");
        }
        // Case 3 — first unpaid instalment: first contact (default).
        return new Qualification("FIRST_CONTACT",
                "First contact; no prior collection action recorded.");
    }

    private static boolean asBool(Object value, boolean fallback) {
        return value instanceof Boolean b ? b : fallback;
    }

    private static String asString(Object value, String fallback) {
        return value instanceof String s && !s.isBlank() ? s : fallback;
    }

    /** Immutable qualification result (code + human-readable reason). */
    private record Qualification(String code, String reason) {}
}
