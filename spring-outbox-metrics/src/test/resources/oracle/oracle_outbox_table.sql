BEGIN
EXECUTE IMMEDIATE '
        CREATE TABLE outbox_events (
            id RAW(16) PRIMARY KEY,
            status VARCHAR2(50) NOT NULL,
            event_type VARCHAR2(255) NOT NULL,
            payload_type VARCHAR2(255) NOT NULL,
            payload CLOB NOT NULL,
            retry_count INTEGER DEFAULT 0,
            next_retry_at TIMESTAMP NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
        )';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_outbox_count ON outbox_events(status, event_type)';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_outbox_pool ON outbox_events(event_type, next_retry_at, id)';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_outbox_recover_and_move_to_dlq ON outbox_events(updated_at, status, id)';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_outbox_cleanup ON outbox_events(updated_at, id)';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/