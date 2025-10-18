package io.github.dmitriyiliyov.springoutbox.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Function;

public final class OutboxRepositoryFactory {

    private static final Logger log = LoggerFactory.getLogger(OutboxRepositoryFactory.class);

    private static final Map<String, Function<DataSource, OutboxRepository>> SUPPORTED_DB = Map.of(
            "postgresql", dataSource -> new PostgreSqlOutboxRepository(getSynchronizedJdbcTemplate(dataSource))
    );

    private OutboxRepositoryFactory() {}

    public static OutboxRepository generate(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            String dbName = conn.getMetaData().getDatabaseProductName().toLowerCase();
            Function<DataSource, OutboxRepository> function = SUPPORTED_DB.get(dbName);
            if (function != null) {
                return function.apply(dataSource);
            } else {
                throw new IllegalArgumentException("Unsupported database: " + dbName);
            }
        } catch (SQLException e) {
            log.error("Error when connecting to dataSource, ", e);
            throw new RuntimeException(e);
        }
    }

    private static JdbcTemplate getSynchronizedJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(new TransactionAwareDataSourceProxy(dataSource));
    }
}
