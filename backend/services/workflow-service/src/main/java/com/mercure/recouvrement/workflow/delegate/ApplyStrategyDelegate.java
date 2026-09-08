package com.mercure.recouvrement.workflow.delegate;

import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared strategy applier for the EPIC 8 workflows.
 *
 * <p>Used via {@code flowable:class} (not a Spring bean) so Flowable instantiates a fresh
 * instance per execution and field injection is safe. Each dedicated process/task injects its
 * own {@code strategy}, {@code status} and {@code workflow} literals, so the per-qualification
 * behavior stays declared in the BPMN rather than duplicated across Java delegates.
 */
public class ApplyStrategyDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ApplyStrategyDelegate.class);

    private Expression strategy;
    private Expression status;
    private Expression workflow;

    @Override
    public void execute(DelegateExecution execution) {
        String strategyValue = resolve(strategy, execution, "FRIENDLY_REMINDER");
        String statusValue = resolve(status, execution, "COMPLETED");
        String workflowValue = resolve(workflow, execution, "");

        execution.setVariable("strategy", strategyValue);
        execution.setVariable("status", statusValue);
        execution.setVariable("workflow", workflowValue);

        log.info("Strategy applied pid={} workflow='{}' strategy={} status={}",
                execution.getProcessInstanceId(), workflowValue, strategyValue, statusValue);
    }

    private static String resolve(Expression expression, DelegateExecution execution, String fallback) {
        if (expression == null) {
            return fallback;
        }
        Object value = expression.getValue(execution);
        return value != null ? value.toString() : fallback;
    }
}
