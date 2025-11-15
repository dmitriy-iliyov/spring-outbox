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

SELECT COUNT(*) INTO @exists
FROM INFORMATION_SCHEMA.STATISTICS
WHERE table_schema = DATABASE()
  AND table_name = 'outbox_events'
  AND index_name = 'idx_outbox_status';

SET @sql = IF(@exists = 0,
    'CREATE INDEX idx_outbox_status ON outbox_events(status);',
    'SELECT "idx_outbox_status exists";');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


SELECT COUNT(*) INTO @exists
FROM INFORMATION_SCHEMA.STATISTICS
WHERE table_schema = DATABASE()
  AND table_name = 'outbox_events'
  AND index_name = 'idx_outbox_status_event_type';

SET @sql = IF(@exists = 0,
    'CREATE INDEX idx_outbox_status_event_type ON outbox_events(status, event_type);',
    'SELECT "idx_outbox_status_event_type exists";');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
FROM INFORMATION_SCHEMA.STATISTICS
WHERE table_schema = DATABASE()
  AND table_name = 'outbox_events'
  AND index_name = 'idx_outbox_status_updated';

SET @sql = IF(@exists = 0,
    'CREATE INDEX idx_outbox_status_updated ON outbox_events(status, updated_at);',
    'SELECT "idx_outbox_status_updated exists";');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;