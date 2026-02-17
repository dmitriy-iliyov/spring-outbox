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
import java.util.Map;
import java.util.function.Function;

public final class OutboxRepositoryFactory {

    private static final Logger log = LoggerFactory.getLogger(OutboxRepositoryFactory.class);
    private static final Map<DatabaseType, Function<JdbcTemplate, OutboxRepository>> SUPPORTED_SUPPLIERS = Map.of(
            DatabaseType.POSTGRESQL,
            jdbcTemplate -> new PostgreSqlOutboxRepository(
                    jdbcTemplate, new PostgreSqlIdHelper(), new DefaultResultSetMapper()
            ),
            DatabaseType.MYSQL,
            jdbcTemplate -> new MySqlOutboxRepository(
                    jdbcTemplate, new MySqlIdHelper(), new DefaultBytesSqlResultSetMapper()
            ),
            DatabaseType.ORACLE,
            jdbcTemplate -> new OracleOutboxRepository(
                    jdbcTemplate, new OracleSqlIdHelper(), new DefaultBytesSqlResultSetMapper()
            )
    );

    private OutboxRepositoryFactory() {}

    public static OutboxRepository generate(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseType databaseType = DatabaseType.fromString(conn.getMetaData().getDatabaseProductName());
            Function<JdbcTemplate, OutboxRepository> supplier = SUPPORTED_SUPPLIERS.get(databaseType);
            if (supplier != null) {
                if (jdbcTemplate == null) {
                    throw new IllegalStateException("JdbcTemplate is null");
                }
                return supplier.apply(jdbcTemplate);
            } else {
                throw new IllegalArgumentException("Supplier for OutboxRepository is null for databaseType=" + databaseType);
            }
        } catch (SQLException e) {
            log.error("Error when connecting to dataSource, ", e);
            throw new RuntimeException(e);
        }
    }
}
