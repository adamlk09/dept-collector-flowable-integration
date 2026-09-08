CREATE TABLE collection_case (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    case_reference VARCHAR(150) NOT NULL,
    customer_id VARCHAR(150) NOT NULL,
    customer_type VARCHAR(50),
    days_overdue INTEGER,
    global_exposure NUMERIC(15, 2),
    ifrs_stage VARCHAR(50),
    collection_segment VARCHAR(50),
    priority VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    assigned_collector VARCHAR(150),
    process_instance_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX idx_collection_case_tenant_reference
    ON collection_case (tenant_id, case_reference);

CREATE INDEX idx_collection_case_tenant_status
    ON collection_case (tenant_id, status);
