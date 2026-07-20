package io.github.dmitriyiliyov.oncebox.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A factory for creating a {@link DatabasePopulator} that initializes the required outbox tables.
 * <p>
 * It detects the database type and selects the appropriate SQL scripts based on the configuration.
 */
public final class OutboxDatabasePopulatorFactory {

    private static final Logger log = LoggerFactory.getLogger(OutboxDatabasePopulatorFactory.class);
    private static final Map<DatabaseType, Map<TableSupplierType, Supplier<Resource>>> OUTBOX_TABLE_SUPPLIERS = Map.of(
            DatabaseType.POSTGRESQL, Map.of(
                    TableSupplierType.OUTBOX, new PostgreSqlOutboxTableSqlResourceSupplier(),
                    TableSupplierType.OUTBOX_JOBS, new PostgreSqlOutboxJobsTableSqlResourceSupplier(),
                    TableSupplierType.OUTBOX_DLQ, new PostgreSqlOutboxDlqTableSqlResourceSupplier(),
                    TableSupplierType.CONSUMED_OUTBOX, new PostgreSqlOutboxConsumedTableSqlResourceSupplier()
            ),
            DatabaseType.MYSQL, Map.of(
                    TableSupplierType.OUTBOX, new MySqlOutboxTableSqlResourceSupplier(),
                    TableSupplierType.OUTBOX_JOBS, new MySqlOutboxJobsTableSqlResourceSupplier(),
                    TableSupplierType.OUTBOX_DLQ, new MySqlOutboxDlqTableSqlResourceSupplier(),
                    TableSupplierType.CONSUMED_OUTBOX, new MySqlOutboxConsumedTableSqlResourceSupplier()
            ),
            DatabaseType.ORACLE, Map.of(
                    TableSupplierType.OUTBOX, new OracleOutboxTableSqlResourceSupplier(),
                    TableSupplierType.OUTBOX_JOBS, new OracleOutboxJobsTableSqlResourceSupplier(),
                    TableSupplierType.OUTBOX_DLQ, new OracleOutboxDlqTableSqlResourceSupplier(),
                    TableSupplierType.CONSUMED_OUTBOX, new OracleOutboxConsumedTableSqlResourceSupplier()
            )
    );

    private OutboxDatabasePopulatorFactory() {}

    /**
     * Generates a {@link DatabasePopulator} based on the provided properties and data source.
     *
     * @param properties             the outbox configuration properties.
     * @param dataSource             the data source to connect to the database.
     * @return                       a configured {@link DatabasePopulator} with the necessary SQL scripts.
     * @throws NullPointerException  if some of the passed parameters is null.
     * @throws RuntimeException      if a database connection cannot be established.
     */
    public static DatabasePopulator create(OutboxProperties properties, DataSource dataSource) {
        Objects.requireNonNull(properties, "properties cannot be null");
        Objects.requireNonNull(dataSource, "dataSource cannot be null");

        List<Resource> scripts = new ArrayList<>();
        DatabaseType databaseType;
        try {
            String dbProductName = JdbcUtils.extractDatabaseMetaData(
                    dataSource, DatabaseMetaData::getDatabaseProductName
            );
            databaseType = DatabaseType.fromString(dbProductName);
            Map<TableSupplierType, Supplier<Resource>> suppliers = OUTBOX_TABLE_SUPPLIERS.get(databaseType);

            Supplier<Resource> outboxSupplier = suppliers.get(TableSupplierType.OUTBOX);
            scripts.add(outboxSupplier.get());

            if (properties.getPublisher().getDlq() != null && properties.getPublisher().getDlq().isEnabled()) {
                Supplier<Resource> dlqSupplier = suppliers.get(TableSupplierType.OUTBOX_DLQ);
                scripts.add(dlqSupplier.get());
            }

            if (properties.getConsumer() != null && properties.getConsumer().isEnabled()) {
                Supplier<Resource> consumedSupplier = suppliers.get(TableSupplierType.CONSUMED_OUTBOX);
                scripts.add(consumedSupplier.get());
            }

            Supplier<Resource> outboxJobsSupplier = suppliers.get(TableSupplierType.OUTBOX_JOBS);
            scripts.add(outboxJobsSupplier.get());

        } catch (MetaDataAccessException e) {
            log.error("Error when getting access to database metadata, ", e);
            throw new RuntimeException(e);
        }
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                false,
                false,
                StandardCharsets.UTF_8.name(),
                scripts.toArray(new Resource[0])
        );
        if (DatabaseType.ORACLE.equals(databaseType)) {
            populator.setSeparator("/");
        }
        return populator;
    }

    private static final class PostgreSqlOutboxTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("psql/psql_outbox_table.sql");
        }
    }

    private static final class PostgreSqlOutboxJobsTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("psql/psql_outbox_jobs_table.sql");
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

    private static final class MySqlOutboxJobsTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("mysql/mysql_outbox_jobs_table.sql");
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

    private static final class OracleOutboxJobsTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("oracle/oracle_outbox_jobs_table.sql");
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
