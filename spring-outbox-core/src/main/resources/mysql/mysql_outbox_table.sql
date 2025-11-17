CREATE TABLE IF NOT EXISTS outbox_events(
    id BINARY(16) PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    retry_count INTEGER DEFAULT 0,
    next_retry_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
);

SET @exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE table_schema = DATABASE()
    AND table_name = 'outbox_events'
    AND index_name = 'idx_outbox_count'
    );

SET @sql := IF(@exists = 0,
    'CREATE INDEX idx_outbox_count ON outbox_events(status, event_type);',
    'SELECT "idx_outbox_count exists";');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE table_schema = DATABASE()
    AND table_name = 'outbox_events'
    AND index_name = 'idx_outbox_pool'
    );

SET @sql := IF(@exists = 0,
    'CREATE INDEX idx_outbox_pool ON outbox_events(event_type, next_retry_at, id) WHERE status = ''PENDING'';',
    'SELECT "idx_outbox_pool exists";');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE table_schema = DATABASE()
    AND table_name = 'outbox_events'
    AND index_name = 'idx_outbox_recover_and_move_to_dlq'
    );

SET @sql := IF(@exists = 0,
    'CREATE INDEX idx_outbox_recover_and_move_to_dlq
         ON outbox_events(updated_at, status, id)
         WHERE status IN (''IN_PROCESS'',''FAILED'');',
    'SELECT "idx_outbox_recover_and_move_to_dlq exists";');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE table_schema = DATABASE()
    AND table_name = 'outbox_events'
    AND index_name = 'idx_outbox_cleanup'
    );

SET @sql := IF(@exists = 0,
    'CREATE INDEX idx_outbox_cleanup ON outbox_events(updated_at, id) WHERE status = ''PROCESSED'';',
    'SELECT "idx_outbox_cleanup exists";');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
