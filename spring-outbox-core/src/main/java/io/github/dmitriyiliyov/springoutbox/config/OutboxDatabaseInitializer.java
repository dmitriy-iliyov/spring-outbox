package io.github.dmitriyiliyov.springoutbox.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public final class OutboxDatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(OutboxDatabaseInitializer.class);
    private static final Map<DatabaseType, OutboxTableSqlSupplier> OUTBOX_TABLE_SUPPLIERS = Map.of(
            DatabaseType.POSTGRESQL, new PostgreSqlOutboxTableSqlSupplier()
    );
    private static final Map<DatabaseType, OutboxDlqTableSqlSupplier> OUTBOX_DLQ_TABLE_SUPPLIERS = Map.of(
            DatabaseType.POSTGRESQL, new PostgreSqlOutboxDlqTableSqlSupplier()
    );

    public static void init(OutboxProperties properties, DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseType databaseType = DatabaseType.fromString(connection.getMetaData().getDatabaseProductName());
            OutboxTableSqlSupplier outboxSupplier = OUTBOX_TABLE_SUPPLIERS.get(databaseType);
            if (outboxSupplier == null) {
                log.error("Unsupported database type: {}", databaseType);
                throw new IllegalStateException("OutboxTableSqlSupplier not found for database: " + databaseType);
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute(outboxSupplier.supply());
                log.info("Table outbox_events successfully created");
                if (properties.getDlq() != null && properties.getDlq().isEnabled()) {
                    OutboxDlqTableSqlSupplier dlqSupplier = OUTBOX_DLQ_TABLE_SUPPLIERS.get(databaseType);
                    if (dlqSupplier == null) {
                        log.error("OutboxDlqTableSqlSupplier not found for database: {}", databaseType);
                        throw new IllegalStateException("OutboxDlqTableSqlSupplier not found for database: " + databaseType);
                    }
                    statement.execute(dlqSupplier.supply());
                    log.info("Table outbox_dlq_events successfully created");
                }
            } catch (SQLException e) {
                log.error("Failed to create tables", e);
                throw new RuntimeException("Failed to create outbox tables", e);
            }
        } catch (SQLException e) {
            log.error("Failed to get database connection", e);
            throw new RuntimeException("Failed to get database connection", e);
        }
    }

    @FunctionalInterface
    private interface SqlSupplier {
        String supply();
    }

    private interface OutboxTableSqlSupplier extends SqlSupplier {}

    private static final class PostgreSqlOutboxTableSqlSupplier implements OutboxTableSqlSupplier {

        @Override
        public String supply() {
            return """
                CREATE TABLE IF NOT EXISTS outbox_events (
                    id UUID PRIMARY KEY,
                    status VARCHAR(50) NOT NULL,
                    event_type VARCHAR(255) NOT NULL,
                    payload_type VARCHAR(255) NOT NULL,
                    payload TEXT NOT NULL,
                    retry_count INTEGER DEFAULT 0,
                    next_retry_at TIMESTAMP NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                );
                
                CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox_events(status);
                CREATE INDEX IF NOT EXISTS idx_outbox_status_event_type ON outbox_events(status, event_type);
                CREATE INDEX IF NOT EXISTS idx_outbox_status_updated ON outbox_events(status, updated_at);
                """;
        }
    }

    private interface OutboxDlqTableSqlSupplier extends SqlSupplier {}

    private static final class PostgreSqlOutboxDlqTableSqlSupplier implements OutboxDlqTableSqlSupplier {

        @Override
        public String supply() {
            return """
                CREATE TABLE IF NOT EXISTS outbox_dlq_events (
                    id UUID PRIMARY KEY,
                    status VARCHAR(50) NOT NULL,
                    dlq_status VARCHAR(50) NOT NULL,
                    event_type VARCHAR(255) NOT NULL,
                    payload_type VARCHAR(255) NOT NULL,
                    payload TEXT NOT NULL,
                    retry_count INTEGER DEFAULT 0,
                    next_retry_at TIMESTAMP NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                );
                
                CREATE INDEX IF NOT EXISTS idx_outbox_dlq_status ON outbox_dlq_events(dlq_status);
                CREATE INDEX IF NOT EXISTS idx_outbox_dlq_id_status ON outbox_dlq_events(id, dlq_status);
                """;
        }
    }
}