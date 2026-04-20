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
    @DisplayName("UT generate() when PostgreSQL should return MultiDialectOutboxDlqWebRepository")
    void generate_whenPostgres_shouldReturnMultiDialectRepository() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        // when
        OutboxDlqWebRepository result = OutboxDlqWebRepositoryFactory.generate(dataSource, jdbcTemplate);

        // then
        assertInstanceOf(MultiDialectOutboxDlqWebRepository.class, result);
    }

    @Test
    @DisplayName("UT generate() when MySQL should return MultiDialectOutboxDlqWebRepository")
    void generate_whenMySql_shouldReturnMultiDialectRepository() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");

        // when
        OutboxDlqWebRepository result = OutboxDlqWebRepositoryFactory.generate(dataSource, jdbcTemplate);

        // then
        assertInstanceOf(MultiDialectOutboxDlqWebRepository.class, result);
    }

    @Test
    @DisplayName("UT generate() when Oracle should return OracleOutboxDlqWebRepository")
    void generate_whenOracle_shouldReturnOracleRepository() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Oracle");

        // when
        OutboxDlqWebRepository result = OutboxDlqWebRepositoryFactory.generate(dataSource, jdbcTemplate);

        // then
        assertInstanceOf(OracleOutboxDlqWebRepository.class, result);
    }

    @Test
    @DisplayName("UT generate() when unsupported DB should throw IAE")
    void generate_whenUnsupportedDb_shouldThrowIAE() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");

        // when + then
        assertThrows(IllegalArgumentException.class, () -> OutboxDlqWebRepositoryFactory.generate(dataSource, jdbcTemplate));
    }

    @Test
    @DisplayName("UT generate() when SQLException should throw RuntimeException")
    void generate_whenSqlException_shouldThrowRuntimeException() throws SQLException {
        // given
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // when + then
        assertThrows(RuntimeException.class, () -> OutboxDlqWebRepositoryFactory.generate(dataSource, jdbcTemplate));
    }

    @Test
    @DisplayName("UT generate() when JdbcTemplate is null should throw IllegalStateException")
    void generate_whenJdbcTemplateIsNull_shouldThrowIllegalStateException() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        // when + then
        assertThrows(IllegalStateException.class, () -> OutboxDlqWebRepositoryFactory.generate(dataSource, null));
    }
}