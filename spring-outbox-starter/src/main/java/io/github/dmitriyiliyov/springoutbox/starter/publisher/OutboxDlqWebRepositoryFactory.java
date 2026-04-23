package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.core.utils.*;
import io.github.dmitriyiliyov.springoutbox.starter.DatabaseType;
import io.github.dmitriyiliyov.springoutbox.web.MultiDialectOutboxDlqWebRepository;
import io.github.dmitriyiliyov.springoutbox.web.OracleOutboxDlqWebRepository;
import io.github.dmitriyiliyov.springoutbox.web.OutboxDlqWebRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * A factory for creating {@link OutboxDlqWebRepository} instances based on the detected database type.
 * <p>
 * It supports PostgreSQL, MySQL, and Oracle.
 */
public final class OutboxDlqWebRepositoryFactory {

    private static final Logger log = LoggerFactory.getLogger(OutboxDlqWebRepositoryFactory.class);
    private static final Map<DatabaseType, OutboxDlqWebRepositoryFactory.OutboxDlqWebRepositorySupplier> SUPPORTED_SUPPLIERS = Map.of(
            DatabaseType.POSTGRESQL, new OutboxDlqWebRepositoryFactory.PostgreSqlOutboxDlqWebRepositorySupplier(),
            DatabaseType.MYSQL, new OutboxDlqWebRepositoryFactory.MySqlOutboxDlqWebRepositorySupplier(),
            DatabaseType.ORACLE, new OutboxDlqWebRepositoryFactory.OracleOutboxDlqWebRepositorySupplier()
    );

    private OutboxDlqWebRepositoryFactory() {}

    /**
     * Generates an {@link OutboxDlqWebRepository} instance based on the database product name.
     *
     * @param dataSource                the data source to get database metadata.
     * @param jdbcTemplate              the JDBC template for database operations.
     * @return                          a configured {@link OutboxDlqWebRepository} instance for the detected database.
     * @throws IllegalArgumentException if the database type is not supported.
     * @throws IllegalStateException    if the {@link JdbcTemplate} is null.
     * @throws RuntimeException         if a database connection cannot be established.
     */
    public static OutboxDlqWebRepository create(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseType databaseType = DatabaseType.fromString(conn.getMetaData().getDatabaseProductName());
            OutboxDlqWebRepositoryFactory.OutboxDlqWebRepositorySupplier supplier = SUPPORTED_SUPPLIERS.get(databaseType);
            if (supplier != null) {
                if (jdbcTemplate == null) {
                    throw new IllegalStateException("JdbcTemplate is null");
                }
                return supplier.supply(jdbcTemplate);
            } else {
                throw new IllegalArgumentException("Supplier for OutboxDlqWebRepository is null for databaseType=" + databaseType);
            }
        } catch (SQLException e) {
            log.error("Error when connecting to dataSource, ", e);
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    public interface OutboxDlqWebRepositorySupplier {
        OutboxDlqWebRepository supply(JdbcTemplate jdbcTemplate);
    }

    public static class PostgreSqlOutboxDlqWebRepositorySupplier implements OutboxDlqWebRepositorySupplier {

        @Override
        public OutboxDlqWebRepository supply(JdbcTemplate jdbcTemplate) {
            return new MultiDialectOutboxDlqWebRepository(
                    jdbcTemplate,
                    new PostgreSqlIdHelper(),
                    new DefaultResultSetMapper()
            );
        }
    }

    public static class MySqlOutboxDlqWebRepositorySupplier implements OutboxDlqWebRepositorySupplier {

        @Override
        public OutboxDlqWebRepository supply(JdbcTemplate jdbcTemplate) {
            return new MultiDialectOutboxDlqWebRepository(
                    jdbcTemplate,
                    new MySqlIdHelper(),
                    new DefaultBytesSqlResultSetMapper()
            );
        }
    }

    public static class OracleOutboxDlqWebRepositorySupplier implements OutboxDlqWebRepositorySupplier {

        @Override
        public OutboxDlqWebRepository supply(JdbcTemplate jdbcTemplate) {
            return new OracleOutboxDlqWebRepository(
                    jdbcTemplate,
                    new OracleSqlIdHelper(),
                    new DefaultBytesSqlResultSetMapper()
            );
        }
    }
}