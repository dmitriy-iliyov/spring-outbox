CREATE TABLE IF NOT EXISTS outbox_consumed_events(
    id UUID PRIMARY KEY,
    consumed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_outbox_consumed_cleanup on outbox_consumed_events(consumed_at, id);