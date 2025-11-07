package io.github.dmitriyiliyov.springoutbox.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OutboxDatabasePopulatorFactory {

    private static final Logger log = LoggerFactory.getLogger(OutboxDatabasePopulatorFactory.class);
    private static final Map<DatabaseType, OutboxTableSqlResourceSupplier> OUTBOX_TABLE_SUPPLIERS = Map.of(
            DatabaseType.POSTGRESQL, new PostgreSqlOutboxTableSqlResourceSupplier()
    );
    private static final Map<DatabaseType, OutboxDlqTableSqlResourceSupplier> OUTBOX_DLQ_TABLE_SUPPLIERS = Map.of(
            DatabaseType.POSTGRESQL, new PostgreSqlOutboxDlqTableSqlResourceSupplier()
    );

    public static DatabasePopulator generate(OutboxProperties properties, DataSource dataSource) {
        List<Resource> scripts = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseType databaseType = DatabaseType.fromString(connection.getMetaData().getDatabaseProductName());
            OutboxTableSqlResourceSupplier outboxSupplier = OUTBOX_TABLE_SUPPLIERS.get(databaseType);
            if (outboxSupplier == null) {
                log.error("Unsupported database type: {}", databaseType);
                throw new IllegalStateException("OutboxTableSqlSupplier not found for database: " + databaseType);
            }
            scripts.add(outboxSupplier.supply());
            if (properties.getDlq() != null && properties.getDlq().isEnabled()) {
                OutboxDlqTableSqlResourceSupplier dlqSupplier = OUTBOX_DLQ_TABLE_SUPPLIERS.get(databaseType);
                if (dlqSupplier == null) {
                    log.error("OutboxDlqTableSqlSupplier not found for database: {}", databaseType);
                    throw new IllegalStateException("OutboxDlqTableSqlSupplier not found for database: " + databaseType);
                }
                scripts.add(dlqSupplier.supply());
            }
        } catch (SQLException e) {
            log.error("Failed to get database connection", e);
            throw new RuntimeException("Failed to get database connection", e);
        }
        return new ResourceDatabasePopulator(
                false,
                false,
                StandardCharsets.UTF_8.name(),
                scripts.toArray(new Resource[0])
        );
    }

    @FunctionalInterface
    private interface SqlResourceSupplier {
        Resource supply();
    }

    private interface OutboxTableSqlResourceSupplier extends SqlResourceSupplier {}

    private interface OutboxDlqTableSqlResourceSupplier extends SqlResourceSupplier {}

    private static final class PostgreSqlOutboxTableSqlResourceSupplier implements OutboxTableSqlResourceSupplier {

        @Override
        public ClassPathResource supply() {
            return new ClassPathResource("psql_outbox_table.sql");
        }
    }

    private static final class PostgreSqlOutboxDlqTableSqlResourceSupplier implements OutboxDlqTableSqlResourceSupplier {

        @Override
        public ClassPathResource supply() {
            return new ClassPathResource("psql_outbox_dlq_table.sql");
        }
    }
}