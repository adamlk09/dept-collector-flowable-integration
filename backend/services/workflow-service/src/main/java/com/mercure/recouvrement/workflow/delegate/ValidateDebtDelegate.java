package com.mercure.recouvrement.workflow.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("validateDebtDelegate")
public class ValidateDebtDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ValidateDebtDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String debtId = (String) execution.getVariable("debtId");
        String customerId = (String) execution.getVariable("customerId");
        Object scoreVar = execution.getVariable("score");

        log.info("Validating debt pid={} debtId={} customerId={} score={}",
                processInstanceId, debtId, customerId, scoreVar);

        if (debtId == null || debtId.isBlank()) {
            throw new IllegalArgumentException("debtId is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId is required");
        }
        if (scoreVar == null) {
            throw new IllegalArgumentException("score is required");
        }
        int score = ((Number) scoreVar).intValue();
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("score must be between 0 and 100 but was " + score);
        }

        execution.setVariable("debtValid", true);
        execution.setVariable("validatedAt", System.currentTimeMillis());

        log.info("Debt validated pid={} debtId={} customerId={} score={}",
                processInstanceId, debtId, customerId, score);
    }
}
