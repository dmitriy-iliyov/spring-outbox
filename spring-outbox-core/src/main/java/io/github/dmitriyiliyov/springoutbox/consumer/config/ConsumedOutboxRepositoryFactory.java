package io.github.dmitriyiliyov.springoutbox.consumer.config;

import io.github.dmitriyiliyov.springoutbox.config.DatabaseType;
import io.github.dmitriyiliyov.springoutbox.config.JdbcTemplateFactory;
import io.github.dmitriyiliyov.springoutbox.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.consumer.PostgreSqlConsumedOutboxRepository;
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
            DatabaseType.POSTGRESQL, dataSource -> new PostgreSqlConsumedOutboxRepository(JdbcTemplateFactory.getSynchronizedJdbcTemplate(dataSource))
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
            log.error("Failed connect to database");
            throw new RuntimeException(e);
        }
    }
}
