CREATE TABLE IF NOT EXISTS outbox_consumed_events(
    id BINARY(16) PRIMARY KEY,
    consumed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_outbox_consumed_at on outbox_consumed_events(consumed_at);