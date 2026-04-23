package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.MySqlOutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OracleOutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.PostgreSqlOutboxDlqRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxDlqRepositoryFactoryUnitTests {

    @Mock
    DataSource dataSource;

    @Mock
    JdbcTemplate jdbcTemplate;

    @Mock
    Connection connection;

    @Mock
    DatabaseMetaData metaData;

    @Test
    @DisplayName("UT create() when PostgreSQL should return PostgreSqlOutboxDlqRepository")
    void create_whenPostgres_shouldReturnPostgreSqlRepository() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        // when
        OutboxDlqRepository result = OutboxDlqRepositoryFactory.create(dataSource, jdbcTemplate);

        // then
        assertInstanceOf(PostgreSqlOutboxDlqRepository.class, result);
    }

    @Test
    @DisplayName("UT create() when MySQL should return MySqlOutboxDlqRepository")
    void create_whenMySql_shouldReturnMySqlRepository() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");

        // when
        OutboxDlqRepository result = OutboxDlqRepositoryFactory.create(dataSource, jdbcTemplate);

        // then
        assertInstanceOf(MySqlOutboxDlqRepository.class, result);
    }

    @Test
    @DisplayName("UT create() when Oracle should return OracleOutboxDlqRepository")
    void create_whenOracle_shouldReturnOracleRepository() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Oracle");

        // when
        OutboxDlqRepository result = OutboxDlqRepositoryFactory.create(dataSource, jdbcTemplate);

        // then
        assertInstanceOf(OracleOutboxDlqRepository.class, result);
    }

    @Test
    @DisplayName("UT create() when unsupported DB should throw IAE")
    void create_whenUnsupportedDb_shouldThrowIAE() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");

        // when + then
        assertThrows(IllegalArgumentException.class, () -> OutboxDlqRepositoryFactory.create(dataSource, jdbcTemplate));
    }

    @Test
    @DisplayName("UT create() when SQLException should throw RuntimeException")
    void create_whenSqlException_shouldThrowRuntimeException() throws SQLException {
        // given
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // when + then
        assertThrows(RuntimeException.class, () -> OutboxDlqRepositoryFactory.create(dataSource, jdbcTemplate));
    }

    @Test
    @DisplayName("UT create() when JdbcTemplate is null should throw IllegalStateException")
    void create_whenJdbcTemplateIsNull_shouldThrowIllegalStateException() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        // when + then
        assertThrows(IllegalStateException.class, () -> OutboxDlqRepositoryFactory.create(dataSource, null));
    }
}