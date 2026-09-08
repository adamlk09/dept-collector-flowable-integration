package com.mercure.recouvrement.workflow.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 2 of bankingRecoveryProcess: KYC / reachability control — validates that a phone number
 * and address are present before the file is staged. Does not block staging on failure (the
 * downstream stage still applies), it only records reachability for the agent's context.
 */
@Component("checkReachabilityDelegate")
public class CheckReachabilityDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CheckReachabilityDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String phoneNumber = (String) execution.getVariable("phoneNumber");
        String address = (String) execution.getVariable("address");

        boolean reachable = phoneNumber != null && !phoneNumber.isBlank()
                && address != null && !address.isBlank();

        execution.setVariable("reachable", reachable);
        execution.setVariable("kycCheckedAt", System.currentTimeMillis());

        log.info("Reachability checked pid={} reachable={} phoneProvided={} addressProvided={}",
                processInstanceId, reachable, phoneNumber != null, address != null);
    }
}
