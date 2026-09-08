package com.mercure.recouvrement.workflow.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.stereotype.Component;

/** doc §12's "Validate records" step — a bad row is skipped (returns null), not fatal to the job. */
@Component
public class IrregularPortfolioItemProcessor implements ItemProcessor<BankingCaseRow, BankingCaseRow> {

    private static final Logger log = LoggerFactory.getLogger(IrregularPortfolioItemProcessor.class);

    @Override
    public BankingCaseRow process(BankingCaseRow row) {
        try {
            if (row.getDebtId() == null || row.getDebtId().isBlank()) {
                throw new ValidationException("debtId is required");
            }
            if (row.getCustomerId() == null || row.getCustomerId().isBlank()) {
                throw new ValidationException("customerId is required");
            }
            if (row.getOverdueDays() == null || row.getOverdueDays() < 0) {
                throw new ValidationException("overdueDays must be >= 0");
            }
            if (row.getTotalExposure() == null || row.getTotalExposure() < 0) {
                throw new ValidationException("totalExposure must be >= 0");
            }
            return row;
        } catch (ValidationException e) {
            log.warn("Skipping invalid row debtId={}: {}", row.getDebtId(), e.getMessage());
            return null;
        }
    }
}
