package com.mercure.recouvrement.workflow.api;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Manual trigger for the synthetic irregular-portfolio ingestion (doc §12), used for the demo/
 * presentation rather than a cron — one call ingests every row of
 * demo-data/banking-cases.csv into new bankingRecoveryProcess instances (idempotent per debtId).
 */
@RestController
@RequestMapping("/api/v1/workflows/batch")
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job irregularPortfolioBatchJob;

    public BatchController(JobLauncher jobLauncher, Job irregularPortfolioBatchJob) {
        this.jobLauncher = jobLauncher;
        this.irregularPortfolioBatchJob = irregularPortfolioBatchJob;
    }

    @PostMapping("/irregular-portfolio/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> run() throws Exception {
        var params = new JobParametersBuilder()
                .addLong("runAt", System.currentTimeMillis())
                .toJobParameters();
        var execution = jobLauncher.run(irregularPortfolioBatchJob, params);
        return Map.of("jobExecutionId", String.valueOf(execution.getId()),
                "status", execution.getStatus().toString());
    }
}
