package com.deptcollector.segmentation.api;

import com.deptcollector.segmentation.api.dto.StagingDecisionRequest;
import com.deptcollector.segmentation.api.dto.StagingDecisionResponse;
import com.deptcollector.segmentation.domain.BankingStagingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/segmentation/staging")
public class StagingController {

    private final BankingStagingService bankingStagingService;

    public StagingController(BankingStagingService bankingStagingService) {
        this.bankingStagingService = bankingStagingService;
    }

    @PostMapping("/execute")
    public StagingDecisionResponse execute(@Valid @RequestBody StagingDecisionRequest request) {
        return bankingStagingService.execute(request);
    }

    @PostMapping("/simulate")
    public StagingDecisionResponse simulate(@Valid @RequestBody StagingDecisionRequest request) {
        return bankingStagingService.simulate(request);
    }
}
