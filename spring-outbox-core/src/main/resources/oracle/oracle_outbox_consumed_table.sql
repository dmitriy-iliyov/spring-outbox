CREATE TABLE outbox_consumed_events(
    id RAW(16) PRIMARY KEY,
    consumed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbox_consumed_at on outbox_consumed_events(consumed_at);