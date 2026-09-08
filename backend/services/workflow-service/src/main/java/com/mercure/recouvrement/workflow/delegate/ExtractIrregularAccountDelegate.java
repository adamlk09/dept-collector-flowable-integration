package com.mercure.recouvrement.workflow.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Step 1 of bankingRecoveryProcess: extraction of the irregular account (anomaly detection —
 * unpaid instalments, debit balances, internal rating downgrade). Placeholder — echoes the
 * request's overdue/exposure figures as-is, same stub style as ValidateDebtDelegate.
 */
@Component("extractIrregularAccountDelegate")
public class ExtractIrregularAccountDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ExtractIrregularAccountDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String debtId = (String) execution.getVariable("debtId");
        Integer overdueDays = (Integer) execution.getVariable("overdueDays");
        Double totalExposure = (Double) execution.getVariable("totalExposure");

        if (overdueDays == null || overdueDays < 0) {
            throw new IllegalArgumentException("overdueDays is required and must be >= 0");
        }
        if (totalExposure == null || totalExposure < 0) {
            throw new IllegalArgumentException("totalExposure is required and must be >= 0");
        }

        execution.setVariable("anomalyType", "IRREGULAR_ACCOUNT");
        execution.setVariable("extractedAt", System.currentTimeMillis());

        log.info("Irregular account extracted pid={} debtId={} overdueDays={} totalExposure={}",
                processInstanceId, debtId, overdueDays, totalExposure);
    }
}
