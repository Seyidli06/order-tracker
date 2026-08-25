CREATE TABLE webhook_audit_log (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    source VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    headers JSONB,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    processing_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_webhook_audit_log_event_id ON webhook_audit_log(event_id);
CREATE INDEX idx_webhook_audit_log_event_type ON webhook_audit_log(event_type);
CREATE INDEX idx_webhook_audit_log_status ON webhook_audit_log(processing_status);
CREATE INDEX idx_webhook_audit_log_received_at ON webhook_audit_log(received_at);

COMMENT ON TABLE webhook_audit_log IS 'Audit log for incoming webhook events';
COMMENT ON COLUMN webhook_audit_log.event_id IS 'Unique identifier from the webhook source';
COMMENT ON COLUMN webhook_audit_log.event_type IS 'Type of webhook event (e.g., order.created, order.updated)';
COMMENT ON COLUMN webhook_audit_log.source IS 'Source system sending the webhook';
COMMENT ON COLUMN webhook_audit_log.payload IS 'Full webhook payload as JSON';
COMMENT ON COLUMN webhook_audit_log.headers IS 'HTTP headers from the webhook request';
COMMENT ON COLUMN webhook_audit_log.processing_status IS 'PENDING, PROCESSED, FAILED';
