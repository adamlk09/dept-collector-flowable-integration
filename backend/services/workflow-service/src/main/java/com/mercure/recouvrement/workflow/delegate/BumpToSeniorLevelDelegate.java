package com.mercure.recouvrement.workflow.delegate;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Task listener (not a service task — runs on {@code escalateToSenior}'s "create" event) that
 * bumps the case up the escalation ladder: doc §23 requires collectorGroup/collectorLevel/
 * priority/collectionPhase to all move together when a case escalates, not just the queue.
 */
@Component("bumpToSeniorLevelDelegate")
public class BumpToSeniorLevelDelegate implements TaskListener {

    private static final Logger log = LoggerFactory.getLogger(BumpToSeniorLevelDelegate.class);

    @Override
    public void notify(DelegateTask delegateTask) {
        String debtId = (String) delegateTask.getVariable("debtId");
        String previousLevel = (String) delegateTask.getVariable("collectorLevel");
        String previousPhase = (String) delegateTask.getVariable("collectionPhase");

        delegateTask.setVariable("collectorGroup", "collectionManagers");
        delegateTask.setVariable("collectorLevel", "SENIOR_COLLECTOR");
        delegateTask.setVariable("priority", "HIGH");
        delegateTask.setVariable("collectionPhase", "HIGH_EXPOSURE");
        delegateTask.setVariable("recoveryTeam", "collectionManagers");

        log.info("Escalated to senior level pid={} debtId={} previousLevel={} previousPhase={} reason={}",
                delegateTask.getProcessInstanceId(), debtId, previousLevel, previousPhase,
                delegateTask.getVariable("escalationReason"));
    }
}
