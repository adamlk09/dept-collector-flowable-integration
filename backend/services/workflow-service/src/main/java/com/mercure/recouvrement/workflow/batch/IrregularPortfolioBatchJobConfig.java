package com.mercure.recouvrement.workflow.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * doc §12: Job / Step / ItemReader / ItemProcessor / ItemWriter, not one giant batch class.
 * Reads the synthetic irregular-portfolio CSV, validates each row, and starts one
 * bankingRecoveryProcess instance per valid row. Restartable/fault-tolerant via Spring Batch's
 * own JobRepository (auto-created — see application.yml's spring.batch.jdbc.initialize-schema).
 */
@Configuration
public class IrregularPortfolioBatchJobConfig {

    @Bean
    public FlatFileItemReader<BankingCaseRow> irregularPortfolioItemReader() {
        return new FlatFileItemReaderBuilder<BankingCaseRow>()
                .name("irregularPortfolioItemReader")
                .resource(new ClassPathResource("demo-data/banking-cases.csv"))
                .linesToSkip(1)
                .lineMapper(lineMapper())
                .build();
    }

    private DefaultLineMapper<BankingCaseRow> lineMapper() {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("debtId", "customerId", "clientSegment", "overdueDays", "totalExposure",
                "phoneNumber", "address");

        BeanWrapperFieldSetMapper<BankingCaseRow> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(BankingCaseRow.class);

        DefaultLineMapper<BankingCaseRow> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return lineMapper;
    }

    @Bean
    public Step irregularPortfolioStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                       FlatFileItemReader<BankingCaseRow> reader,
                                       ItemProcessor<BankingCaseRow, BankingCaseRow> irregularPortfolioItemProcessor,
                                       ItemWriter<BankingCaseRow> irregularPortfolioItemWriter) {
        return new StepBuilder("irregularPortfolioStep", jobRepository)
                .<BankingCaseRow, BankingCaseRow>chunk(5, transactionManager)
                .reader(reader)
                .processor(irregularPortfolioItemProcessor)
                .writer(irregularPortfolioItemWriter)
                .faultTolerant()
                .skipLimit(50)
                .skip(Exception.class)
                .build();
    }

    @Bean
    public Job irregularPortfolioBatchJob(JobRepository jobRepository, Step irregularPortfolioStep) {
        return new JobBuilder("IrregularPortfolioBatchJob", jobRepository)
                .start(irregularPortfolioStep)
                .build();
    }

}
