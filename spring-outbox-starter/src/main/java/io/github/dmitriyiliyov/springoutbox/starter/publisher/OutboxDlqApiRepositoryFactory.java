package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.core.utils.*;
import io.github.dmitriyiliyov.springoutbox.dlq.api.MySqlOutboxDlqApiRepository;
import io.github.dmitriyiliyov.springoutbox.dlq.api.OracleOutboxDlqApiRepository;
import io.github.dmitriyiliyov.springoutbox.dlq.api.OutboxDlqApiRepository;
import io.github.dmitriyiliyov.springoutbox.dlq.api.PostgreSqlOutboxDlqApiRepository;
import io.github.dmitriyiliyov.springoutbox.starter.DatabaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Map;

/**
 * A factory for creating {@link OutboxDlqApiRepository} instances based on the detected database type.
 * <p>
 * It supports PostgreSQL, MySQL, and Oracle.
 */
public final class OutboxDlqApiRepositoryFactory {

    private static final Logger log = LoggerFactory.getLogger(OutboxDlqApiRepositoryFactory.class);
    private static final Map<DatabaseType, OutboxDlqApiRepositorySupplier> SUPPORTED_SUPPLIERS = Map.of(
            DatabaseType.POSTGRESQL, new PostgreSqlOutboxDlqApiRepositorySupplier(),
            DatabaseType.MYSQL, new MySqlOutboxDlqApiRepositorySupplier(),
            DatabaseType.ORACLE, new OracleOutboxDlqApiRepositorySupplier()
    );

    private OutboxDlqApiRepositoryFactory() {}

    /**
     * Generates an {@link OutboxDlqApiRepository} instance based on the database product name.
     *
     * @param dataSource                the data source to get database metadata.
     * @param jdbcTemplate              the JDBC template for database operations.
     * @return                          a configured {@link OutboxDlqApiRepository} instance for the detected database.
     * @throws IllegalArgumentException if the database type is not supported.
     * @throws IllegalStateException    if the {@link JdbcTemplate} is null.
     * @throws RuntimeException         if a database connection cannot be established.
     */
    public static OutboxDlqApiRepository create(DataSource dataSource, JdbcTemplate jdbcTemplate, Clock clock) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseType databaseType = DatabaseType.fromString(conn.getMetaData().getDatabaseProductName());
            OutboxDlqApiRepositorySupplier supplier = SUPPORTED_SUPPLIERS.get(databaseType);
            if (supplier != null) {
                if (jdbcTemplate == null) {
                    throw new IllegalStateException("JdbcTemplate is null");
                }
                return supplier.supply(jdbcTemplate, clock);
            } else {
                throw new IllegalArgumentException("Supplier for OutboxDlqApiRepository is null for databaseType=" + databaseType);
            }
        } catch (SQLException e) {
            log.error("Error when connecting to dataSource, ", e);
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    public interface OutboxDlqApiRepositorySupplier {
        OutboxDlqApiRepository supply(JdbcTemplate jdbcTemplate, Clock clock);
    }

    public static class PostgreSqlOutboxDlqApiRepositorySupplier implements OutboxDlqApiRepositorySupplier {

        @Override
        public OutboxDlqApiRepository supply(JdbcTemplate jdbcTemplate, Clock clock) {
            return new PostgreSqlOutboxDlqApiRepository(
                    jdbcTemplate,
                    new PostgreSqlIdHelper(),
                    new DefaultResultSetMapper(),
                    clock
            );
        }
    }

    public static class MySqlOutboxDlqApiRepositorySupplier implements OutboxDlqApiRepositorySupplier {

        @Override
        public OutboxDlqApiRepository supply(JdbcTemplate jdbcTemplate, Clock clock) {
            return new MySqlOutboxDlqApiRepository(
                    jdbcTemplate,
                    new MySqlIdHelper(),
                    new DefaultBytesResultSetMapper(),
                    clock
            );
        }
    }

    public static class OracleOutboxDlqApiRepositorySupplier implements OutboxDlqApiRepositorySupplier {

        @Override
        public OutboxDlqApiRepository supply(JdbcTemplate jdbcTemplate, Clock clock) {
            return new OracleOutboxDlqApiRepository(
                    jdbcTemplate,
                    new OracleSqlIdHelper(),
                    new DefaultBytesResultSetMapper(),
                    clock
            );
        }
    }
}