CREATE TABLE IF NOT EXISTS outbox_dlq_events (
    id BINARY(16) PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    dlq_status VARCHAR(50) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    retry_count INTEGER DEFAULT 0,
    next_retry_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    moved_at DATATIME NOT NULL
);

CREATE INDEX idx_outbox_dlq_status ON outbox_dlq_events(dlq_status);
CREATE INDEX idx_outbox_dlq_id_status ON outbox_dlq_events(id, dlq_status);