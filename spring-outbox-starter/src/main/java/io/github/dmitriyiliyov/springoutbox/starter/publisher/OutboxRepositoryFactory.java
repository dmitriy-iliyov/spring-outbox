package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.core.publisher.MySqlOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OracleOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.PostgreSqlOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.utils.*;
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
 * A factory for creating {@link OutboxRepository} instances based on the detected database type.
 * <p>
 * It supports PostgreSQL, MySQL, and Oracle.
 */
public final class OutboxRepositoryFactory {

    private static final Logger log = LoggerFactory.getLogger(OutboxRepositoryFactory.class);
    private static final Map<DatabaseType, OutboxRepositorySupplier> SUPPORTED_SUPPLIERS = Map.of(
            DatabaseType.POSTGRESQL, new PostgreSqlOutboxRepositorySupplier(),
            DatabaseType.MYSQL, new MySqlOutboxRepositorySupplier(),
            DatabaseType.ORACLE, new OracleOutboxRepositorySupplier()
    );

    private OutboxRepositoryFactory() {}

    /**
     * Generates an {@link OutboxRepository} instance based on the database product name.
     *
     * @param dataSource                the data source to get database metadata.
     * @param jdbcTemplate              the JDBC template for database operations.
     * @return                          a configured {@link OutboxRepository} instance for the detected database.
     * @throws IllegalArgumentException if the database type is not supported.
     * @throws IllegalStateException    if the {@link JdbcTemplate} is null.
     * @throws RuntimeException         if a database connection cannot be established.
     */
    public static OutboxRepository generate(DataSource dataSource, JdbcTemplate jdbcTemplate, Clock clock) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseType databaseType = DatabaseType.fromString(conn.getMetaData().getDatabaseProductName());
            OutboxRepositorySupplier supplier = SUPPORTED_SUPPLIERS.get(databaseType);
            if (supplier != null) {
                if (jdbcTemplate == null) {
                    throw new IllegalStateException("JdbcTemplate is null");
                }
                if (clock == null) {
                    throw new IllegalStateException("Clock is null");
                }
                return supplier.supply(jdbcTemplate, clock);
            } else {
                throw new IllegalArgumentException("Supplier for OutboxRepository is null for databaseType=" + databaseType);
            }
        } catch (SQLException e) {
            log.error("Error when connecting to dataSource, ", e);
            throw new RuntimeException(e);
        }
    }

    public interface OutboxRepositorySupplier {
        OutboxRepository supply(JdbcTemplate jdbcTemplate, Clock clock);
    }

    public static class PostgreSqlOutboxRepositorySupplier implements OutboxRepositorySupplier {

        @Override
        public OutboxRepository supply(JdbcTemplate jdbcTemplate, Clock clock) {
            return new PostgreSqlOutboxRepository(
                    jdbcTemplate,
                    clock,
                    new PostgreSqlIdHelper(),
                    new DefaultResultSetMapper()
            );
        }
    }

    public static class MySqlOutboxRepositorySupplier implements OutboxRepositorySupplier {

        @Override
        public OutboxRepository supply(JdbcTemplate jdbcTemplate, Clock clock) {
            return new MySqlOutboxRepository(
                    jdbcTemplate,
                    clock,
                    new MySqlIdHelper(),
                    new DefaultBytesSqlResultSetMapper()
            );
        }
    }

    public static class OracleOutboxRepositorySupplier implements OutboxRepositorySupplier {

        @Override
        public OutboxRepository supply(JdbcTemplate jdbcTemplate, Clock clock) {
            return new OracleOutboxRepository(
                    jdbcTemplate,
                    clock,
                    new OracleSqlIdHelper(),
                    new DefaultBytesSqlResultSetMapper()
            );
        }
    }
}
