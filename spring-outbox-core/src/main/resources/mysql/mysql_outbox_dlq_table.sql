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

SELECT COUNT(*) INTO @exists
FROM INFORMATION_SCHEMA.STATISTICS
WHERE table_schema = DATABASE()
  AND table_name = 'outbox_dlq_events'
  AND index_name = 'idx_outbox_dlq_status';

SET @sql = IF(
    @exists = 0,
    'CREATE INDEX idx_outbox_dlq_status ON outbox_dlq_events(dlq_status);',
    'SELECT "idx_outbox_dlq_status exists";'
    );

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
FROM INFORMATION_SCHEMA.STATISTICS
WHERE table_schema = DATABASE()
  AND table_name = 'outbox_dlq_events'
  AND index_name = 'idx_outbox_dlq_id_status';

SET @sql = IF(
    @exists = 0,
    'CREATE INDEX idx_outbox_dlq_id_status ON outbox_dlq_events(id, dlq_status);',
    'SELECT "idx_outbox_dlq_id_status exists";'
    );

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
