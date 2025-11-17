CREATE TABLE IF NOT EXISTS outbox_events(
    id RAW(16) PRIMARY KEY,
    status VARCHAR2(50) NOT NULL,
    event_type VARCHAR2(255) NOT NULL,
    payload_type VARCHAR2(255) NOT NULL,
    payload VARCHAR2(4000) NOT NULL,
    retry_count NUMBER DEFAULT 0,
    next_retry_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_count ON outbox_events(status, event_type);
CREATE INDEX IF NOT EXISTS idx_outbox_pool
    ON outbox_events(event_type, next_retry_at, id)
    WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_outbox_recover_and_move_to_dlq
    ON outbox_events(updated_at, status, id)
    WHERE status IN ('IN_PROCESS', 'FAILED');
CREATE INDEX IF NOT EXISTS idx_outbox_cleanup
    ON outbox_events(updated_at, id)
    WHERE status = 'PROCESSED';