package io.github.dmitriyiliyov.springoutbox.config;

import io.github.dmitriyiliyov.springoutbox.core.OutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.PostgreSqlOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Function;

public final class OutboxRepositoryFactory {

    private static final Logger log = LoggerFactory.getLogger(OutboxRepositoryFactory.class);
    private static final Map<String, Function<DataSource, OutboxRepository>> SUPPORTED_SUPPLIERS = Map.of(
            "postgresql",
            dataSource -> new PostgreSqlOutboxRepository(JdbcTemplateFactory.getSynchronizedJdbcTemplate(dataSource))
    );

    private OutboxRepositoryFactory() {}

    public static OutboxRepository generate(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            String dbName = conn.getMetaData().getDatabaseProductName().toLowerCase();
            Function<DataSource, OutboxRepository> supplier = SUPPORTED_SUPPLIERS.get(dbName);
            if (supplier != null) {
                return supplier.apply(dataSource);
            } else {
                throw new IllegalArgumentException("Unsupported database: " + dbName);
            }
        } catch (SQLException e) {
            log.error("Error when connecting to dataSource, ", e);
            throw new RuntimeException(e);
        }
    }
}
