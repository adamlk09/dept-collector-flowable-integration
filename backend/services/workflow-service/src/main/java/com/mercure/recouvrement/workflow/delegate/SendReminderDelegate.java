package com.mercure.recouvrement.workflow.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fired by the non-interrupting boundary timer on {@code assignToAgent} — the human task is
 * still open (cancelActivity="false") when this runs, it just records that an SLA reminder was
 * sent while the agent hasn't acted yet.
 */
@Component("sendReminderDelegate")
public class SendReminderDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(SendReminderDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        String debtId = (String) execution.getVariable("debtId");
        execution.setVariable("reminderSentAt", System.currentTimeMillis());
        log.info("SLA reminder sent pid={} debtId={} — assignToAgent still open",
                execution.getProcessInstanceId(), debtId);
    }
}
