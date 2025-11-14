package io.github.dmitriyiliyov.springoutbox.publisher.config;

import io.github.dmitriyiliyov.springoutbox.config.DatabaseType;
import io.github.dmitriyiliyov.springoutbox.config.JdbcTemplateFactory;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.MySqlOutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.PostgreSqlOutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.MySqlResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.PostgreSqlResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.utils.MySqlIdHelper;
import io.github.dmitriyiliyov.springoutbox.utils.PostgreSqlIdHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Function;

public final class OutboxDlqRepositoryFactory {

    private static final Logger log = LoggerFactory.getLogger(OutboxDlqRepositoryFactory.class);
    private static final Map<DatabaseType, Function<DataSource, OutboxDlqRepository>> SUPPORTED_SUPPLIERS = Map.of(
            DatabaseType.POSTGRESQL,
            dataSource -> new PostgreSqlOutboxDlqRepository(
                    JdbcTemplateFactory.getSynchronizedJdbcTemplate(dataSource),
                    new PostgreSqlIdHelper(),
                    new PostgreSqlResultSetMapper()
            ),
            DatabaseType.MYSQL,
            dataSource -> new MySqlOutboxDlqRepository(
                    JdbcTemplateFactory.getSynchronizedJdbcTemplate(dataSource),
                    new MySqlIdHelper(),
                    new MySqlResultSetMapper()
            )
    );

    private OutboxDlqRepositoryFactory() {}

    public static OutboxDlqRepository generate(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseType databaseType = DatabaseType.fromString(conn.getMetaData().getDatabaseProductName());
            Function<DataSource, OutboxDlqRepository> supplier = SUPPORTED_SUPPLIERS.get(databaseType);
            if (supplier != null) {
                return supplier.apply(dataSource);
            } else {
                throw new IllegalArgumentException("Supplier for OutboxDlqRepository is null for databaseType=" + databaseType);
            }
        } catch (SQLException e) {
            log.error("Error when connecting to dataSource, ", e);
            throw new RuntimeException(e);
        }
    }
}