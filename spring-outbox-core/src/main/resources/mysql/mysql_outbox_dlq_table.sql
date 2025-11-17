CREATE TABLE IF NOT EXISTS outbox_dlq_events (
    id BINARY(16) PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    dlq_status VARCHAR(50) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    retry_count INTEGER DEFAULT 0,
    next_retry_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    moved_at DATETIME NOT NULL
);

SET @exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE table_schema = DATABASE()
    AND table_name = 'outbox_dlq_events'
    AND index_name = 'idx_outbox_dlq_count'
    );

SET @sql := IF(@exists = 0,
    'CREATE INDEX idx_outbox_dlq_count ON outbox_dlq_events(event_type, dlq_status);',
    'SELECT "idx_outbox_dlq_count exists";'
    );

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE table_schema = DATABASE()
    AND table_name = 'outbox_dlq_events'
    AND index_name = 'idx_outbox_dlq_move_to_main'
    );

SET @sql := IF(@exists = 0,
    'CREATE INDEX idx_outbox_dlq_move_to_main
         ON outbox_dlq_events(moved_at, id)
         WHERE dlq_status = ''TO_RETRY'';',
    'SELECT "idx_outbox_dlq_move_to_main exists";'
    );

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
