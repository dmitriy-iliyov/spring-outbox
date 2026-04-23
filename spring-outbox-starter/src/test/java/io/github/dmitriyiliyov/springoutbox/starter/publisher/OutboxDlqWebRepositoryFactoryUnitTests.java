package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.web.MultiDialectOutboxDlqWebRepository;
import io.github.dmitriyiliyov.springoutbox.web.OracleOutboxDlqWebRepository;
import io.github.dmitriyiliyov.springoutbox.web.OutboxDlqWebRepository;
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
class OutboxDlqWebRepositoryFactoryUnitTests {

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
        OutboxDlqWebRepository result = OutboxDlqWebRepositoryFactory.create(dataSource, jdbcTemplate);

        // then
        assertInstanceOf(MultiDialectOutboxDlqWebRepository.class, result);
    }

    @Test
    @DisplayName("UT create() when MySQL should return MultiDialectOutboxDlqWebRepository")
    void create_whenMySql_shouldReturnMultiDialectRepository() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");

        // when
        OutboxDlqWebRepository result = OutboxDlqWebRepositoryFactory.create(dataSource, jdbcTemplate);

        // then
        assertInstanceOf(MultiDialectOutboxDlqWebRepository.class, result);
    }

    @Test
    @DisplayName("UT create() when Oracle should return OracleOutboxDlqWebRepository")
    void create_whenOracle_shouldReturnOracleRepository() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Oracle");

        // when
        OutboxDlqWebRepository result = OutboxDlqWebRepositoryFactory.create(dataSource, jdbcTemplate);

        // then
        assertInstanceOf(OracleOutboxDlqWebRepository.class, result);
    }

    @Test
    @DisplayName("UT create() when unsupported DB should throw IAE")
    void create_whenUnsupportedDb_shouldThrowIAE() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");

        // when + then
        assertThrows(IllegalArgumentException.class, () -> OutboxDlqWebRepositoryFactory.create(dataSource, jdbcTemplate));
    }

    @Test
    @DisplayName("UT create() when SQLException should throw RuntimeException")
    void create_whenSqlException_shouldThrowRuntimeException() throws SQLException {
        // given
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // when + then
        assertThrows(RuntimeException.class, () -> OutboxDlqWebRepositoryFactory.create(dataSource, jdbcTemplate));
    }

    @Test
    @DisplayName("UT create() when JdbcTemplate is null should throw IllegalStateException")
    void create_whenJdbcTemplateIsNull_shouldThrowIllegalStateException() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        // when + then
        assertThrows(IllegalStateException.class, () -> OutboxDlqWebRepositoryFactory.create(dataSource, null));
    }
}
