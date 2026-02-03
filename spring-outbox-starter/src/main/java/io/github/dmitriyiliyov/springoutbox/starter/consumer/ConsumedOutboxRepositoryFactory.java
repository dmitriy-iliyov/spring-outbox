package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.consumer.MySqlConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.consumer.OracleConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.consumer.PostgreSqlConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.utils.DefaultBytesSqlResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.core.utils.MySqlIdHelper;
import io.github.dmitriyiliyov.springoutbox.core.utils.OracleSqlIdHelper;
import io.github.dmitriyiliyov.springoutbox.starter.DatabaseType;
import io.github.dmitriyiliyov.springoutbox.starter.JdbcTemplateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class ConsumedOutboxRepositoryFactory {

    private static final Logger log = LoggerFactory.getLogger(ConsumedOutboxRepositoryFactory.class);
    private static final Map<DatabaseType, Function<DataSource, ConsumedOutboxRepository>> SUPPORTED_DB = Map.of(
            DatabaseType.POSTGRESQL, dataSource -> new PostgreSqlConsumedOutboxRepository(
                    JdbcTemplateFactory.getSynchronizedJdbcTemplate(dataSource)
            ),
            DatabaseType.MYSQL, dataSource -> new MySqlConsumedOutboxRepository(
                    JdbcTemplateFactory.getSynchronizedJdbcTemplate(dataSource),
                    new MySqlIdHelper(),
                    new DefaultBytesSqlResultSetMapper()
            ),
            DatabaseType.ORACLE, dataSource -> new OracleConsumedOutboxRepository(
                    JdbcTemplateFactory.getSynchronizedJdbcTemplate(dataSource),
                    new OracleSqlIdHelper(),
                    new DefaultBytesSqlResultSetMapper()
            )
    );

    public static ConsumedOutboxRepository generate(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource cannot be null");
        try (Connection connection = dataSource.getConnection()) {
            DatabaseType databaseType = DatabaseType.fromString(connection.getMetaData().getDatabaseProductName());
            Function<DataSource, ConsumedOutboxRepository> supplier = SUPPORTED_DB.get(databaseType);
            if (supplier == null) {
                log.error("Supplier is null because database is unsupported");
                throw new IllegalArgumentException("Unsupported database " + databaseType);
            }
            return supplier.apply(dataSource);
        } catch (Exception e) {
            log.error("Error when connecting to database");
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e);
        }
    }
}
