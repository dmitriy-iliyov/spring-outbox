CREATE TABLE outbox_events(
    id RAW(16) PRIMARY KEY,
    status VARCHAR2(50) NOT NULL,
    event_type VARCHAR2(255) NOT NULL,
    payload_type VARCHAR2(255) NOT NULL,
    payload VARCHAR2 NOT NULL,
    retry_count NUMBER DEFAULT 0,
    next_retry_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_outbox_status ON outbox_events(status);
CREATE INDEX idx_outbox_status_event_type ON outbox_events(status, event_type);
CREATE INDEX idx_outbox_status_updated ON outbox_events(status, updated_at);