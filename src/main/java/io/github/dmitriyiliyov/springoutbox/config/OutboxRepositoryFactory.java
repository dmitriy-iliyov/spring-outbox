package io.github.dmitriyiliyov.springoutbox.config;

import io.github.dmitriyiliyov.springoutbox.core.OutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.PostgreSqlOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.dlq.PostgreSqlOutboxDlqRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public final class OutboxRepositoryFactory {

    private static final Logger log = LoggerFactory.getLogger(OutboxRepositoryFactory.class);
    private static final Object SUPPLIER_LOCK = new Object();
    private static volatile OutboxRepositoriesSupplier SUPPLIER;
    private static final Map<String, OutboxRepositoriesSupplier> SUPPORTED_DB = Map.of(
            "postgresql", new PostgreSqlOutboxRepositoriesSupplier()
    );

    private OutboxRepositoryFactory() {}

    public static OutboxRepositoriesSupplier.OutboxRepositoriesPair generate(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            String dbName = conn.getMetaData().getDatabaseProductName().toLowerCase();
            if (SUPPLIER == null) {
                synchronized (SUPPLIER_LOCK) {
                    if (SUPPLIER == null) {
                        SUPPLIER = SUPPORTED_DB.get(dbName);
                    }
                }
            }
            if (SUPPLIER != null) {
                return SUPPLIER.supply(dataSource);
            } else {
                throw new IllegalArgumentException("Unsupported database: " + dbName);
            }
        } catch (SQLException e) {
            log.error("Error when connecting to dataSource, ", e);
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    private interface OutboxRepositoriesSupplier {

        record OutboxRepositoriesPair(
                OutboxRepository main,
                OutboxDlqRepository dlq
        ){}

        OutboxRepositoriesPair supply(DataSource dataSource);
    }

    private static final class PostgreSqlOutboxRepositoriesSupplier implements OutboxRepositoriesSupplier {

        private volatile JdbcTemplate jdbcTemplate;

        @Override
        public OutboxRepositoriesPair supply(DataSource dataSource) {
            if (jdbcTemplate == null) {
                synchronized (this) {
                    if (jdbcTemplate == null) {
                        jdbcTemplate = new JdbcTemplate(new TransactionAwareDataSourceProxy(dataSource));
                    }
                }
            }
            return new OutboxRepositoriesPair(
                    new PostgreSqlOutboxRepository(jdbcTemplate),
                    new PostgreSqlOutboxDlqRepository(jdbcTemplate)
            );
        }
    }
}
