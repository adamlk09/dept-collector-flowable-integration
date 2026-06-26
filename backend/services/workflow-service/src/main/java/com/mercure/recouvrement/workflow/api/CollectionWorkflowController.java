package com.mercure.recouvrement.workflow.api;

import com.mercure.recouvrement.workflow.application.CollectionWorkflowService;
import com.mercure.recouvrement.workflow.dto.CompleteTaskRequest;
import com.mercure.recouvrement.workflow.dto.IncidentDto;
import com.mercure.recouvrement.workflow.dto.JobDto;
import com.mercure.recouvrement.workflow.dto.ProcessHistoryDto;
import com.mercure.recouvrement.workflow.dto.StartCollectionProcessRequest;
import com.mercure.recouvrement.workflow.dto.StartCollectionProcessResponse;
import com.mercure.recouvrement.workflow.dto.StartProcessResponse;
import com.mercure.recouvrement.workflow.dto.TaskDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workflows")
public class CollectionWorkflowController {

    private final CollectionWorkflowService workflowService;

    public CollectionWorkflowController(CollectionWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/collection/start")
    public ResponseEntity<StartCollectionProcessResponse> startCollection(
            @Valid @RequestBody StartCollectionProcessRequest request) {
        StartCollectionProcessResponse response = workflowService.startCollectionProcess(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/processes")
    public ResponseEntity<StartProcessResponse> startProcess(
            @Valid @RequestBody StartCollectionProcessRequest request) {
        StartProcessResponse response = workflowService.startProcess(request);
        return ResponseEntity
                .status(response.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/tasks")
    public List<TaskDto> getActiveTasks() {
        return workflowService.getActiveTasks();
    }

    @PostMapping("/tasks/{taskId}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completeTask(@PathVariable String taskId,
                             @RequestBody(required = false) CompleteTaskRequest request) {
        workflowService.completeTask(taskId, request != null ? request.variables() : Map.of());
    }

    @GetMapping("/processes/{processInstanceId}/history")
    public List<ProcessHistoryDto> getProcessHistory(@PathVariable String processInstanceId) {
        return workflowService.getProcessHistory(processInstanceId);
    }

    @GetMapping("/processes/{processInstanceId}/variables")
    public Map<String, Object> getVariables(@PathVariable String processInstanceId) {
        return workflowService.getVariables(processInstanceId);
    }

    @GetMapping("/processes/{processInstanceId}/incidents")
    public List<IncidentDto> getIncidents(@PathVariable String processInstanceId) {
        return workflowService.getIncidents(processInstanceId);
    }

    @GetMapping("/processes/{processInstanceId}/jobs/dead")
    public List<JobDto> getDeadLetterJobs(@PathVariable String processInstanceId) {
        return workflowService.getDeadLetterJobs(processInstanceId);
    }

    @PostMapping("/jobs/{jobId}/retry")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retryDeadLetterJob(@PathVariable String jobId) {
        workflowService.retryDeadLetterJob(jobId);
    }

    @GetMapping("/processes/{processInstanceId}/timers")
    public List<JobDto> getTimers(@PathVariable String processInstanceId) {
        return workflowService.getTimers(processInstanceId);
    }

    @PostMapping("/timers/{timerId}/trigger")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void triggerTimer(@PathVariable String timerId) {
        workflowService.triggerTimer(timerId);
    }
}
