package io.github.dmitriyiliyov.springoutbox.config;

import io.github.dmitriyiliyov.springoutbox.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.dlq.PostgreSqlOutboxDlqRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Function;

public final class OutboxDlqRepositoryFactory {

    private static final Logger log = LoggerFactory.getLogger(OutboxDlqRepositoryFactory.class);
    private static final Map<String, Function<DataSource, OutboxDlqRepository>> SUPPORTED_SUPPLIERS = Map.of(
            "postgresql",
            dataSource -> new PostgreSqlOutboxDlqRepository(JdbcTemplateFactory.getSynchronizedJdbcTemplate(dataSource))
    );

    private OutboxDlqRepositoryFactory() {}

    public static OutboxDlqRepository generate(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            String dbName = conn.getMetaData().getDatabaseProductName().toLowerCase();
            Function<DataSource, OutboxDlqRepository> supplier = SUPPORTED_SUPPLIERS.get(dbName);
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