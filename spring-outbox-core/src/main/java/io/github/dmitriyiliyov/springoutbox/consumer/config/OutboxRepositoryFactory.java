package io.github.dmitriyiliyov.springoutbox.consumer.config;

import io.github.dmitriyiliyov.springoutbox.config.DatabaseType;
import io.github.dmitriyiliyov.springoutbox.config.JdbcTemplateFactory;
import io.github.dmitriyiliyov.springoutbox.consumer.OutboxRepository;
import io.github.dmitriyiliyov.springoutbox.consumer.PostgreSqlOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class OutboxRepositoryFactory {

    private static final Logger log = LoggerFactory.getLogger(OutboxRepositoryFactory.class);
    private static final Map<DatabaseType, Function<DataSource, OutboxRepository>> SUPPORTED_DB = Map.of(
            DatabaseType.POSTGRESQL, dataSource -> new PostgreSqlOutboxRepository(JdbcTemplateFactory.getSynchronizedJdbcTemplate(dataSource))
    );

    public static OutboxRepository generate(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource cannot be null");
        try (Connection connection = dataSource.getConnection()) {
            DatabaseType databaseType = DatabaseType.fromString(connection.getMetaData().getDatabaseProductName());
            Function<DataSource, OutboxRepository> supplier = SUPPORTED_DB.get(databaseType);
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
