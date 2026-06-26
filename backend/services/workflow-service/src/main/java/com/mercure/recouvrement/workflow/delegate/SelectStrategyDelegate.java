package com.mercure.recouvrement.workflow.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("selectStrategyDelegate")
public class SelectStrategyDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(SelectStrategyDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        String debtId  = (String) execution.getVariable("debtId");
        String segment = (String) execution.getVariable("segment");

        log.info("Selecting strategy debtId={} segment={}", debtId, segment);

        String strategy = switch (segment != null ? segment : "STANDARD") {
            case "CRITICAL"  -> "LEGAL_ACTION";
            case "HIGH"      -> "INTENSIVE_CONTACT";
            case "MEDIUM"    -> "STANDARD_CONTACT";
            default          -> "FRIENDLY_REMINDER";
        };

        execution.setVariable("strategy", strategy);
        execution.setVariable("strategySelectedAt", System.currentTimeMillis());

        log.info("Strategy selected debtId={} strategy={}", debtId, strategy);
    }
}
