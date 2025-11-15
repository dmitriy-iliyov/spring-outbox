package io.github.dmitriyiliyov.springoutbox.publisher.config;

import io.github.dmitriyiliyov.springoutbox.config.DatabaseType;
import io.github.dmitriyiliyov.springoutbox.config.JdbcTemplateFactory;
import io.github.dmitriyiliyov.springoutbox.publisher.MySqlOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.publisher.OracleOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.publisher.OutboxRepository;
import io.github.dmitriyiliyov.springoutbox.publisher.PostgreSqlOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.DefaultBytesSqlResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.DefaultResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.utils.MySqlIdHelper;
import io.github.dmitriyiliyov.springoutbox.utils.OracleSqlIdHelper;
import io.github.dmitriyiliyov.springoutbox.utils.PostgreSqlIdHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Function;

public final class OutboxRepositoryFactory {

    private static final Logger log = LoggerFactory.getLogger(OutboxRepositoryFactory.class);
    private static final Map<DatabaseType, Function<DataSource, OutboxRepository>> SUPPORTED_SUPPLIERS = Map.of(
            DatabaseType.POSTGRESQL,
            dataSource -> new PostgreSqlOutboxRepository(
                    JdbcTemplateFactory.getSynchronizedJdbcTemplate(dataSource),
                    new PostgreSqlIdHelper(),
                    new DefaultResultSetMapper()
            ),
            DatabaseType.MYSQL,
            dataSource -> new MySqlOutboxRepository(
                    JdbcTemplateFactory.getSynchronizedJdbcTemplate(dataSource),
                    new MySqlIdHelper(),
                    new DefaultBytesSqlResultSetMapper()
            ),
            DatabaseType.ORACLE,
            dataSource -> new OracleOutboxRepository(
                    JdbcTemplateFactory.getSynchronizedJdbcTemplate(dataSource),
                    new OracleSqlIdHelper(),
                    new DefaultBytesSqlResultSetMapper()
            )
    );

    private OutboxRepositoryFactory() {}

    public static OutboxRepository generate(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseType databaseType = DatabaseType.fromString(conn.getMetaData().getDatabaseProductName());
            Function<DataSource, OutboxRepository> supplier = SUPPORTED_SUPPLIERS.get(databaseType);
            if (supplier != null) {
                return supplier.apply(dataSource);
            } else {
                throw new IllegalArgumentException("Supplier for OutboxRepository is null for databaseType=" + databaseType);
            }
        } catch (SQLException e) {
            log.error("Error when connecting to dataSource, ", e);
            throw new RuntimeException(e);
        }
    }
}
