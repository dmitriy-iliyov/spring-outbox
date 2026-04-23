package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.consumer.MySqlConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.consumer.OracleConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.consumer.PostgreSqlConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.utils.DefaultBytesSqlResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.core.utils.MySqlIdHelper;
import io.github.dmitriyiliyov.springoutbox.core.utils.OracleSqlIdHelper;
import io.github.dmitriyiliyov.springoutbox.starter.DatabaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Clock;
import java.util.Map;
import java.util.Objects;

/**
 * A factory for creating {@link ConsumedOutboxRepository} instances based on the detected database type.
 * <p>
 * It supports PostgreSQL, MySQL, Oracle.
 */
public final class ConsumedOutboxRepositoryFactory {

    private static final Logger log = LoggerFactory.getLogger(ConsumedOutboxRepositoryFactory.class);
    private static final Map<DatabaseType, ConsumedOutboxRepositorySupplier> SUPPORTED_DB = Map.of(
            DatabaseType.POSTGRESQL, new PostgreSqlConsumedOutboxRepositorySupplier(),
            DatabaseType.MYSQL, new MySqlConsumedOutboxRepositorySupplier(),
            DatabaseType.ORACLE, new OracleConsumedOutboxRepositorySupplier()
    );

    private ConsumedOutboxRepositoryFactory() {}

    /**
     * Generates a {@link ConsumedOutboxRepository} instance based on the database product name.
     *
     * @param dataSource                the data source to get database metadata.
     * @param jdbcTemplate              the JDBC template for database operations.
     * @return                          a configured {@link ConsumedOutboxRepository} instance for the detected database.
     * @throws IllegalArgumentException if the database type is not supported.
     * @throws IllegalStateException    if the {@link JdbcTemplate} is null.
     * @throws RuntimeException         if a database connection cannot be established.
     */
    public static ConsumedOutboxRepository create(DataSource dataSource, JdbcTemplate jdbcTemplate, Clock clock) {
        Objects.requireNonNull(dataSource, "dataSource cannot be null");
        try (Connection connection = dataSource.getConnection()) {
            DatabaseType databaseType = DatabaseType.fromString(connection.getMetaData().getDatabaseProductName());
            ConsumedOutboxRepositorySupplier supplier = SUPPORTED_DB.get(databaseType);
            if (supplier == null) {
                log.error("Supplier is null because database is unsupported");
                throw new IllegalArgumentException("Unsupported database " + databaseType);
            }
            if (jdbcTemplate == null) {
                throw new IllegalStateException("JdbcTemplate is null");
            }
            if (clock == null) {
                throw new IllegalStateException("Clock is null");
            }
            return supplier.supply(jdbcTemplate, clock);
        } catch (Exception e) {
            log.error("Error when connecting to database");
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e);
        }
    }

    public interface ConsumedOutboxRepositorySupplier {
        ConsumedOutboxRepository supply(JdbcTemplate jdbcTemplate, Clock clock);
    }

    public static class PostgreSqlConsumedOutboxRepositorySupplier implements ConsumedOutboxRepositorySupplier {

        @Override
        public ConsumedOutboxRepository supply(JdbcTemplate jdbcTemplate, Clock clock) {
            return new PostgreSqlConsumedOutboxRepository(jdbcTemplate, clock);
        }
    }

    public static class MySqlConsumedOutboxRepositorySupplier implements ConsumedOutboxRepositorySupplier {

        @Override
        public ConsumedOutboxRepository supply(JdbcTemplate jdbcTemplate, Clock clock) {
            return new MySqlConsumedOutboxRepository(
                    jdbcTemplate,
                    clock,
                    new MySqlIdHelper(),
                    new DefaultBytesSqlResultSetMapper()
            );
        }
    }

    public static class OracleConsumedOutboxRepositorySupplier implements ConsumedOutboxRepositorySupplier{

        @Override
        public ConsumedOutboxRepository supply(JdbcTemplate jdbcTemplate, Clock clock) {
            return new OracleConsumedOutboxRepository(
                    jdbcTemplate,
                    clock,
                    new OracleSqlIdHelper(),
                    new DefaultBytesSqlResultSetMapper()
            );
        }
    }
}
