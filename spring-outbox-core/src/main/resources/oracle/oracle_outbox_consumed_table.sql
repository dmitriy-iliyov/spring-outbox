CREATE TABLE IF NOT EXISTS outbox_consumed_events(
    id RAW(16) PRIMARY KEY,
    consumed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_consumed_cleanup on outbox_consumed_events(consumed_at, id);