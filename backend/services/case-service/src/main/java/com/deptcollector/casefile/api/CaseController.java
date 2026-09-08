package com.deptcollector.casefile.api;

import com.deptcollector.casefile.api.dto.CaseResponse;
import com.deptcollector.casefile.api.dto.CreateCaseRequest;
import com.deptcollector.casefile.api.dto.UpdateCaseStatusRequest;
import com.deptcollector.casefile.domain.CollectionCase;
import com.deptcollector.casefile.domain.CollectionCaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cases")
public class CaseController {

    private final CollectionCaseService service;

    public CaseController(CollectionCaseService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CaseResponse> create(@Valid @RequestBody CreateCaseRequest request) {
        CollectionCase created = service.createOrGet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CaseResponse.from(created));
    }

    @GetMapping
    public List<CaseResponse> list() {
        return service.list().stream().map(CaseResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CaseResponse get(@PathVariable UUID id) {
        return CaseResponse.from(service.get(id));
    }

    @PatchMapping("/{id}/status")
    public CaseResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateCaseStatusRequest request) {
        return CaseResponse.from(service.updateStatus(id, request.status(), request.assignedCollector()));
    }
}
