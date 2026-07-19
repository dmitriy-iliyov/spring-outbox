CREATE TABLE IF NOT EXISTS e2e_produced_events (
    verify_id UUID PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS e2e_consumed_events (
    verify_id UUID NOT NULL
);
