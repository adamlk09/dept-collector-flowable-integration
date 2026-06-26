package com.mercure.recouvrement.workflow.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Standard path (score &lt; 70): assigns the standard collection strategy.
 */
@Component("selectStandardStrategyDelegate")
public class SelectStandardStrategyDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(SelectStandardStrategyDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String segment = (String) execution.getVariable("segment");

        execution.setVariable("strategy", "STANDARD_COLLECTION");
        execution.setVariable("status", "COMPLETED");

        log.info("Standard strategy selected pid={} segment={} strategy=STANDARD_COLLECTION",
                processInstanceId, segment);
    }
}
