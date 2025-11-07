CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    retry_count INTEGER DEFAULT 0,
    next_retry_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox_events(status);
CREATE INDEX IF NOT EXISTS idx_outbox_status_event_type ON outbox_events(status, event_type);
CREATE INDEX IF NOT EXISTS idx_outbox_status_updated ON outbox_events(status, updated_at);