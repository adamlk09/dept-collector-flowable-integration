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

        log.info("Validating debt pid={} debtId={} customerId={}",
                processInstanceId, debtId, customerId);

        if (debtId == null || debtId.isBlank()) {
            throw new IllegalArgumentException("debtId is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId is required");
        }

        execution.setVariable("debtValid", true);
        execution.setVariable("validatedAt", System.currentTimeMillis());

        log.info("Debt validated pid={} debtId={} customerId={}",
                processInstanceId, debtId, customerId);
    }
}
