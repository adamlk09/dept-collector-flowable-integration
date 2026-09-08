package com.mercure.recouvrement.workflow.api;

import com.mercure.recouvrement.workflow.application.CollectionWorkflowService;
import com.mercure.recouvrement.workflow.dto.AssignTaskRequest;
import com.mercure.recouvrement.workflow.dto.CloseActionRequest;
import com.mercure.recouvrement.workflow.dto.CompleteTaskRequest;
import com.mercure.recouvrement.workflow.dto.ContactCustomerActionRequest;
import com.mercure.recouvrement.workflow.dto.EscalateActionRequest;
import com.mercure.recouvrement.workflow.dto.FormalNoticeActionRequest;
import com.mercure.recouvrement.workflow.dto.IncidentDto;
import com.mercure.recouvrement.workflow.dto.JobDto;
import com.mercure.recouvrement.workflow.dto.PaymentPlanActionRequest;
import com.mercure.recouvrement.workflow.dto.PaymentRequest;
import com.mercure.recouvrement.workflow.dto.ProcessHistoryDto;
import com.mercure.recouvrement.workflow.dto.PromisePaymentActionRequest;
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

import java.util.HashMap;
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

    @PostMapping("/tasks/{taskId}/assign")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignTask(@PathVariable String taskId,
                           @Valid @RequestBody AssignTaskRequest request) {
        workflowService.assignTask(taskId, request.assignee());
    }

    @PostMapping("/tasks/{taskId}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completeTask(@PathVariable String taskId,
                             @RequestBody(required = false) CompleteTaskRequest request) {
        workflowService.completeTask(taskId, request != null ? request.variables() : Map.of());
    }

    // --- Business actions (doc §17): dedicated, self-documenting endpoints instead of one
    // generic "complete" call. Each is a thin wrapper — validate, set process variables,
    // complete the task via the existing generic completeTask (never a parallel task engine).

    @PostMapping("/tasks/{taskId}/actions/promise-payment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void promisePayment(@PathVariable String taskId, @RequestBody PromisePaymentActionRequest request) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("paymentPromiseAmount", request.amount());
        vars.put("paymentPromiseDate", request.promiseDate());
        vars.put("lastActionType", "PROMISE_PAYMENT");
        workflowService.completeTask(taskId, vars);
    }

    @PostMapping("/tasks/{taskId}/actions/payment-plan")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void paymentPlan(@PathVariable String taskId, @RequestBody PaymentPlanActionRequest request) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("paymentPlanAmount", request.amount());
        vars.put("paymentPlanInstallments", request.installments());
        vars.put("lastActionType", "PAYMENT_PLAN");
        workflowService.completeTask(taskId, vars);
    }

    @PostMapping("/tasks/{taskId}/actions/contact-customer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void contactCustomer(@PathVariable String taskId, @RequestBody ContactCustomerActionRequest request) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("lastContactDate", System.currentTimeMillis());
        vars.put("contactNotes", request.notes());
        vars.put("lastActionType", "CONTACT_CUSTOMER");
        workflowService.completeTask(taskId, vars);
    }

    @PostMapping("/tasks/{taskId}/actions/formal-notice")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void formalNotice(@PathVariable String taskId, @RequestBody(required = false) FormalNoticeActionRequest request) {
        workflowService.formalNoticeAction(taskId, request != null ? request.notes() : null);
    }

    @PostMapping("/tasks/{taskId}/actions/escalate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void escalate(@PathVariable String taskId, @RequestBody EscalateActionRequest request) {
        workflowService.escalateAction(taskId, request.reason());
    }

    @PostMapping("/tasks/{taskId}/actions/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@PathVariable String taskId, @RequestBody(required = false) CloseActionRequest request) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("caseClosedByAgent", true);
        vars.put("closeNotes", request != null ? request.notes() : null);
        vars.put("lastActionType", "CLOSE");
        workflowService.completeTask(taskId, vars);
    }

    @PostMapping("/cases/{caseId}/payment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordPayment(@PathVariable String caseId,
                              @RequestBody PaymentRequest request) {
        workflowService.correlatePayment(caseId, request.amount(), request.paymentDate());
    }

    @GetMapping("/processes/{processInstanceId}/history")
    public List<ProcessHistoryDto> getProcessHistory(@PathVariable String processInstanceId) {
        return workflowService.getProcessHistory(processInstanceId);
    }

    @GetMapping("/processes/{processInstanceId}/variables")
    public Map<String, Object> getVariables(@PathVariable String processInstanceId) {
        return workflowService.getVariables(processInstanceId);
    }

    @PostMapping("/processes/{processInstanceId}/variables")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setVariables(@PathVariable String processInstanceId,
                             @RequestBody(required = false) Map<String, Object> variables) {
        workflowService.setVariables(processInstanceId, variables);
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
