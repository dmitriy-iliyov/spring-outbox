BEGIN
EXECUTE IMMEDIATE '
        CREATE TABLE outbox_dlq_events (
            id RAW(16) PRIMARY KEY,
            status VARCHAR2(50) NOT NULL,
            dlq_status VARCHAR2(50) NOT NULL,
            event_type VARCHAR2(255) NOT NULL,
            payload_type VARCHAR2(255) NOT NULL,
            payload CLOB NOT NULL,
            retry_count INTEGER DEFAULT 0,
            next_retry_at TIMESTAMP NOT NULL,
            created_at TIMESTAMP NOT NULL,
            updated_at TIMESTAMP NOT NULL,
            moved_at TIMESTAMP NOT NULL
        )';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_outbox_dlq_count ON outbox_dlq_events(event_type, dlq_status)';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_outbox_dlq_move_to_main ON outbox_dlq_events(moved_at, id)';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/