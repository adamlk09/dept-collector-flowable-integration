package com.mercure.recouvrement.workflow.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("segmentDebtDelegate")
public class SegmentDebtDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(SegmentDebtDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        int score = ((Number) execution.getVariable("score")).intValue();

        String segment = score >= 70 ? "HIGH_RISK" : "LOW_RISK";

        execution.setVariable("segment", segment);
        execution.setVariable("segmentedAt", System.currentTimeMillis());

        log.info("Debt segmented pid={} score={} segment={}", processInstanceId, score, segment);
    }
}
