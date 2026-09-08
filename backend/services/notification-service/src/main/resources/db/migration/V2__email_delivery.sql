CREATE TABLE email_delivery (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    trigger_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(1000),
    sent_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_email_delivery_tenant
    ON email_delivery (tenant_id, sent_at DESC);
