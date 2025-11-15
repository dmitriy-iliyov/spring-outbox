CREATE TABLE IF NOT EXISTS outbox_dlq_events (
    id RAW(16) PRIMARY KEY,
    status VARCHAR2(50) NOT NULL,
    dlq_status VARCHAR2(50) NOT NULL,
    event_type VARCHAR2(255) NOT NULL,
    payload_type VARCHAR2(255) NOT NULL,
    payload VARCHAR2(4000) NOT NULL,
    retry_count NUMBER DEFAULT 0,
    next_retry_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    moved_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_dlq_status ON outbox_dlq_events(dlq_status);
CREATE INDEX IF NOT EXISTS idx_outbox_dlq_id_status ON outbox_dlq_events(id, dlq_status);