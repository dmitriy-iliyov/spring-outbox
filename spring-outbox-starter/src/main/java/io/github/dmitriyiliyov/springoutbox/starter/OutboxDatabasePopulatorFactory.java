package io.github.dmitriyiliyov.springoutbox.starter;

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
import java.util.function.Supplier;

public final class OutboxDatabasePopulatorFactory {

    private static final Logger log = LoggerFactory.getLogger(OutboxDatabasePopulatorFactory.class);
    private static final Map<DatabaseType, Map<SupplierType, Supplier<Resource>>> OUTBOX_TABLE_SUPPLIERS = Map.of(
            DatabaseType.POSTGRESQL, Map.of(
                    SupplierType.OUTBOX_TABLE, new PostgreSqlOutboxTableSqlResourceSupplier(),
                    SupplierType.OUTBOX_DLQ_TABLE, new PostgreSqlOutboxDlqTableSqlResourceSupplier(),
                    SupplierType.CONSUMED_OUTBOX_TABLE, new PostgreSqlOutboxConsumedTableSqlResourceSupplier()
            ),
            DatabaseType.MYSQL, Map.of(
                    SupplierType.OUTBOX_TABLE, new MySqlOutboxTableSqlResourceSupplier(),
                    SupplierType.OUTBOX_DLQ_TABLE, new MySqlOutboxDlqTableSqlResourceSupplier(),
                    SupplierType.CONSUMED_OUTBOX_TABLE, new MySqlOutboxConsumedTableSqlResourceSupplier()
            ),
            DatabaseType.ORACLE, Map.of(
                    SupplierType.OUTBOX_TABLE, new OracleOutboxTableSqlResourceSupplier(),
                    SupplierType.OUTBOX_DLQ_TABLE, new OracleOutboxDlqTableSqlResourceSupplier(),
                    SupplierType.CONSUMED_OUTBOX_TABLE, new OracleOutboxConsumedTableSqlResourceSupplier()
            )
    );

    public static DatabasePopulator generate(OutboxProperties properties, DataSource dataSource) {
        List<Resource> scripts = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseType databaseType = DatabaseType.fromString(connection.getMetaData().getDatabaseProductName());
            Map<SupplierType, Supplier<Resource>> suppliers = OUTBOX_TABLE_SUPPLIERS.get(databaseType);
            if (suppliers == null) {
                log.error("Unsupported database type: {}", databaseType);
                throw new IllegalStateException("Suppliers not found for database: " + databaseType);
            }
            Supplier<Resource> outboxSupplier = suppliers.get(SupplierType.OUTBOX_TABLE);
            if (outboxSupplier == null) {
                log.error("Unsupported database: {}", databaseType);
                throw new IllegalStateException("Supplier for outbox_events not found, database: " + databaseType);
            }
            scripts.add(outboxSupplier.get());
            if (properties.getPublisher().getDlq() != null && properties.getPublisher().getDlq().isEnabled()) {
                Supplier<Resource> dlqSupplier = suppliers.get(SupplierType.OUTBOX_DLQ_TABLE);
                if (dlqSupplier == null) {
                    log.error("Supplier for outbox_dlq_events not found, database: {}", databaseType);
                    throw new IllegalStateException("Supplier for outbox_dlq_events not found, database: " + databaseType);
                }
                scripts.add(dlqSupplier.get());
            }
            if (properties.getConsumer() != null && properties.getConsumer().isEnabled()) {
                Supplier<Resource> consumedSupplier = suppliers.get(SupplierType.CONSUMED_OUTBOX_TABLE);
                if (consumedSupplier == null) {
                    log.error("Supplier for outbox_consumed_events not found, database: {}", databaseType);
                    throw new IllegalStateException("Supplier for outbox_consumed_events not found, database: " + databaseType);
                }
                scripts.add(consumedSupplier.get());
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

    private static final class PostgreSqlOutboxTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("psql/psql_outbox_table.sql");
        }
    }

    private static final class PostgreSqlOutboxDlqTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("psql/psql_outbox_dlq_table.sql");
        }
    }

    private static final class PostgreSqlOutboxConsumedTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("psql/psql_outbox_consumed_table.sql");
        }
    }

    private static final class MySqlOutboxTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("mysql/mysql_outbox_table.sql");
        }
    }

    private static final class MySqlOutboxDlqTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("mysql/mysql_outbox_dlq_table.sql");
        }
    }

    private static final class MySqlOutboxConsumedTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("mysql/mysql_outbox_consumed_table.sql");
        }
    }

    private static final class OracleOutboxTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("oracle/oracle_outbox_table.sql");
        }
    }

    private static final class OracleOutboxDlqTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("oracle/oracle_outbox_dlq_table.sql");
        }
    }

    private static final class OracleOutboxConsumedTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("oracle/oracle_outbox_consumed_table.sql");
        }
    }
}