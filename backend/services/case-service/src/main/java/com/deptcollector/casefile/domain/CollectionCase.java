package com.deptcollector.casefile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "collection_case")
public class CollectionCase {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "case_reference", nullable = false)
    private String caseReference;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "customer_type")
    private String customerType;

    @Column(name = "days_overdue")
    private Integer daysOverdue;

    @Column(name = "global_exposure")
    private BigDecimal globalExposure;

    @Column(name = "ifrs_stage")
    private String ifrsStage;

    @Column(name = "collection_segment")
    private String collectionSegment;

    @Column(name = "priority")
    private String priority;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "assigned_collector")
    private String assignedCollector;

    @Column(name = "process_instance_id")
    private String processInstanceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CollectionCase() {
    }

    public CollectionCase(UUID id, String tenantId, String caseReference, String customerId,
                          String customerType, Integer daysOverdue, BigDecimal globalExposure,
                          String ifrsStage, String collectionSegment, String priority,
                          String processInstanceId, Instant now) {
        this.id = id;
        this.tenantId = tenantId;
        this.caseReference = caseReference;
        this.customerId = customerId;
        this.customerType = customerType;
        this.daysOverdue = daysOverdue;
        this.globalExposure = globalExposure;
        this.ifrsStage = ifrsStage;
        this.collectionSegment = collectionSegment;
        this.priority = priority;
        this.status = "OPEN";
        this.processInstanceId = processInstanceId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateStatus(String status, String assignedCollector, Instant now) {
        this.status = status;
        if (assignedCollector != null) {
            this.assignedCollector = assignedCollector;
        }
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCustomerType() {
        return customerType;
    }

    public Integer getDaysOverdue() {
        return daysOverdue;
    }

    public BigDecimal getGlobalExposure() {
        return globalExposure;
    }

    public String getIfrsStage() {
        return ifrsStage;
    }

    public String getCollectionSegment() {
        return collectionSegment;
    }

    public String getPriority() {
        return priority;
    }

    public String getStatus() {
        return status;
    }

    public String getAssignedCollector() {
        return assignedCollector;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
