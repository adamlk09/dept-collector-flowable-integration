package com.mercure.recouvrement.workflow.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * High-risk path (score &gt;= 70): assigns the priority collection strategy.
 */
@Component("selectPriorityStrategyDelegate")
public class SelectPriorityStrategyDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(SelectPriorityStrategyDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String segment = (String) execution.getVariable("segment");

        execution.setVariable("strategy", "PRIORITY_COLLECTION");
        execution.setVariable("status", "COMPLETED");

        log.info("Priority strategy selected pid={} segment={} strategy=PRIORITY_COLLECTION",
                processInstanceId, segment);
    }
}
