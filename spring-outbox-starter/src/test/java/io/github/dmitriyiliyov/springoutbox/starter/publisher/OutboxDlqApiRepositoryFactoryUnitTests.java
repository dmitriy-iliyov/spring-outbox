package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.dlq.api.MultiDialectOutboxDlqApiRepository;
import io.github.dmitriyiliyov.springoutbox.dlq.api.OracleOutboxDlqApiRepository;
import io.github.dmitriyiliyov.springoutbox.dlq.api.OutboxDlqApiRepository;
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
class OutboxDlqApiRepositoryFactoryUnitTests {

    @Mock
    DataSource dataSource;

    @Mock
    JdbcTemplate jdbcTemplate;

    @Mock
    Connection connection;

    @Mock
    DatabaseMetaData metaData;

    @Test
    @DisplayName("UT create() when PostgreSQL should return MultiDialectOutboxDlqWebRepository")
    void create_whenPostgres_shouldReturnMultiDialectRepository() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        // when
        OutboxDlqApiRepository result = OutboxDlqApiRepositoryFactory.create(dataSource, jdbcTemplate);

        // then
        assertInstanceOf(MultiDialectOutboxDlqApiRepository.class, result);
    }

    @Test
    @DisplayName("UT create() when MySQL should return MultiDialectOutboxDlqWebRepository")
    void create_whenMySql_shouldReturnMultiDialectRepository() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");

        // when
        OutboxDlqApiRepository result = OutboxDlqApiRepositoryFactory.create(dataSource, jdbcTemplate);

        // then
        assertInstanceOf(MultiDialectOutboxDlqApiRepository.class, result);
    }

    @Test
    @DisplayName("UT create() when Oracle should return OracleOutboxDlqWebRepository")
    void create_whenOracle_shouldReturnOracleRepository() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Oracle");

        // when
        OutboxDlqApiRepository result = OutboxDlqApiRepositoryFactory.create(dataSource, jdbcTemplate);

        // then
        assertInstanceOf(OracleOutboxDlqApiRepository.class, result);
    }

    @Test
    @DisplayName("UT create() when unsupported DB should throw IAE")
    void create_whenUnsupportedDb_shouldThrowIAE() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");

        // when + then
        assertThrows(IllegalArgumentException.class, () -> OutboxDlqApiRepositoryFactory.create(dataSource, jdbcTemplate));
    }

    @Test
    @DisplayName("UT create() when SQLException should throw RuntimeException")
    void create_whenSqlException_shouldThrowRuntimeException() throws SQLException {
        // given
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // when + then
        assertThrows(RuntimeException.class, () -> OutboxDlqApiRepositoryFactory.create(dataSource, jdbcTemplate));
    }

    @Test
    @DisplayName("UT create() when JdbcTemplate is null should throw IllegalStateException")
    void create_whenJdbcTemplateIsNull_shouldThrowIllegalStateException() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        // when + then
        assertThrows(IllegalStateException.class, () -> OutboxDlqApiRepositoryFactory.create(dataSource, null));
    }
}
