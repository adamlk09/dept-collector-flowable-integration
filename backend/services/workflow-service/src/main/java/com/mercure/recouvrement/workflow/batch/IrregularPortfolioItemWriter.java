package com.mercure.recouvrement.workflow.batch;

import com.mercure.recouvrement.workflow.application.BankingRecoveryWorkflowService;
import com.mercure.recouvrement.workflow.dto.StartBankingRecoveryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * doc §12's "Start Flowable process instance" step — one {@code bankingRecoveryProcess} per row,
 * via the existing idempotent {@link BankingRecoveryWorkflowService#startProcess}, so re-running
 * the same CSV (a restarted/failed batch job) never creates duplicate cases.
 */
@Component
public class IrregularPortfolioItemWriter implements ItemWriter<BankingCaseRow> {

    private static final Logger log = LoggerFactory.getLogger(IrregularPortfolioItemWriter.class);

    private final BankingRecoveryWorkflowService workflowService;

    public IrregularPortfolioItemWriter(BankingRecoveryWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Override
    public void write(Chunk<? extends BankingCaseRow> chunk) {
        for (BankingCaseRow row : chunk) {
            var response = workflowService.startProcess(new StartBankingRecoveryRequest(
                    row.getDebtId(), row.getCustomerId(), row.getClientSegment(), row.getOverdueDays(),
                    row.getTotalExposure(), row.getPhoneNumber(), row.getAddress(), null, null));
            log.info("Ingested debtId={} processInstanceId={} created={}",
                    row.getDebtId(), response.processInstanceId(), response.created());
        }
    }
}
