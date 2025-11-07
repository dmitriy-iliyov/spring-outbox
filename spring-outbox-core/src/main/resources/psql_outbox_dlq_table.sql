CREATE TABLE IF NOT EXISTS outbox_dlq_events (
    id UUID PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    dlq_status VARCHAR(50) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    retry_count INTEGER DEFAULT 0,
    next_retry_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_outbox_dlq_status ON outbox_dlq_events(dlq_status);
CREATE INDEX IF NOT EXISTS idx_outbox_dlq_id_status ON outbox_dlq_events(id, dlq_status);