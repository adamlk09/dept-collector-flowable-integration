package com.deptcollector.segmentation.api;

import com.deptcollector.segmentation.api.dto.DecisionRequest;
import com.deptcollector.segmentation.api.dto.DecisionResponse;
import com.deptcollector.segmentation.domain.SegmentationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/segmentation")
public class SegmentationController {

    private final SegmentationService segmentationService;

    public SegmentationController(SegmentationService segmentationService) {
        this.segmentationService = segmentationService;
    }

    // F3 + F4 — execute DMN and return qualification + rule explanation
    @PostMapping("/execute")
    public DecisionResponse execute(@Valid @RequestBody DecisionRequest request) {
        return segmentationService.execute(request);
    }

    // F3 — simulate DMN with no side effects (same evaluation, simulated=true in response)
    @PostMapping("/simulate")
    public DecisionResponse simulate(@Valid @RequestBody DecisionRequest request) {
        return segmentationService.simulate(request);
    }
}
