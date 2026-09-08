package com.mercure.recouvrement.workflow.delegate;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Starts the {@code bankingCollectionCase} CMMN case for the STAGE3_LITIGATION branch.
 *
 * <p>Flowable has no native BPMN "call CMMN case" element, so starting a case from a process is
 * done here via {@link CmmnRuntimeService} from a plain service task — the standard BPMN/CMMN
 * integration pattern. The BPMN flow continues unchanged afterwards (into {@code applyLegalTransfer});
 * this only opens the discretionary case alongside it, it doesn't replace the deterministic path.
 */
@Component("startLegalCaseDelegate")
public class StartLegalCaseDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(StartLegalCaseDelegate.class);
    private static final String CASE_KEY = "bankingCollectionCase";

    private final CmmnRuntimeService cmmnRuntimeService;

    public StartLegalCaseDelegate(CmmnRuntimeService cmmnRuntimeService) {
        this.cmmnRuntimeService = cmmnRuntimeService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String debtId = (String) execution.getVariable("debtId");

        Map<String, Object> caseVars = new HashMap<>();
        caseVars.put("debtId", debtId);
        caseVars.put("customerId", execution.getVariable("customerId"));
        caseVars.put("stage", execution.getVariable("stage"));
        caseVars.put("priority", execution.getVariable("priority"));
        caseVars.put("action", execution.getVariable("action"));
        caseVars.put("tenantId", execution.getVariable("tenantId"));
        caseVars.put("bpmnProcessInstanceId", execution.getProcessInstanceId());

        String cmmnCaseInstanceId = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey(CASE_KEY)
                .businessKey(debtId)
                .variables(caseVars)
                .start()
                .getId();

        execution.setVariable("cmmnCaseInstanceId", cmmnCaseInstanceId);
        log.info("Started CMMN case caseInstanceId={} debtId={} bpmnPid={}",
                cmmnCaseInstanceId, debtId, execution.getProcessInstanceId());
    }
}
