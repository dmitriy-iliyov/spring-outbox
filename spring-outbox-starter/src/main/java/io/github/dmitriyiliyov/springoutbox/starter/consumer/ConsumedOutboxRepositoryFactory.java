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
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class ConsumedOutboxRepositoryFactory {

    private static final Logger log = LoggerFactory.getLogger(ConsumedOutboxRepositoryFactory.class);
    private static final Map<DatabaseType, Function<JdbcTemplate, ConsumedOutboxRepository>> SUPPORTED_DB = Map.of(
            DatabaseType.POSTGRESQL, PostgreSqlConsumedOutboxRepository::new,
            DatabaseType.MYSQL, jdbcTemplate -> new MySqlConsumedOutboxRepository(
                    jdbcTemplate, new MySqlIdHelper(), new DefaultBytesSqlResultSetMapper()
            ),
            DatabaseType.ORACLE, jdbcTemplate -> new OracleConsumedOutboxRepository(
                    jdbcTemplate, new OracleSqlIdHelper(), new DefaultBytesSqlResultSetMapper()
            )
    );

    public static ConsumedOutboxRepository generate(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        Objects.requireNonNull(dataSource, "dataSource cannot be null");
        try (Connection connection = dataSource.getConnection()) {
            DatabaseType databaseType = DatabaseType.fromString(connection.getMetaData().getDatabaseProductName());
            Function<JdbcTemplate, ConsumedOutboxRepository> supplier = SUPPORTED_DB.get(databaseType);
            if (supplier == null) {
                log.error("Supplier is null because database is unsupported");
                throw new IllegalArgumentException("Unsupported database " + databaseType);
            }
            if (jdbcTemplate == null) {
                throw new IllegalStateException("JdbcTemplate is null");
            }
            return supplier.apply(jdbcTemplate);
        } catch (Exception e) {
            log.error("Error when connecting to database");
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e);
        }
    }
}
