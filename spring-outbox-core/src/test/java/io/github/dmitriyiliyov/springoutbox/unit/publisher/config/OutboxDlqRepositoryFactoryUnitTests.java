package io.github.dmitriyiliyov.springoutbox.unit.publisher.config;

import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxDlqRepositoryFactory;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.MySqlOutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.OracleOutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.PostgreSqlOutboxDlqRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    Connection connection;

    @Mock
    DatabaseMetaData metaData;

    @Test
    @DisplayName("UT generate() when PostgreSQL should return PostgreSqlOutboxDlqRepository")
    void generate_whenPostgres_shouldReturnPostgreSqlRepository() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        // when
        OutboxDlqRepository result = OutboxDlqRepositoryFactory.generate(dataSource);

        // then
        assertInstanceOf(PostgreSqlOutboxDlqRepository.class, result);
    }

    @Test
    @DisplayName("UT generate() when MySQL should return MySqlOutboxDlqRepository")
    void generate_whenMySql_shouldReturnMySqlRepository() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");

        // when
        OutboxDlqRepository result = OutboxDlqRepositoryFactory.generate(dataSource);

        // then
        assertInstanceOf(MySqlOutboxDlqRepository.class, result);
    }

    @Test
    @DisplayName("UT generate() when Oracle should return OracleOutboxDlqRepository")
    void generate_whenOracle_shouldReturnOracleRepository() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Oracle");

        // when
        OutboxDlqRepository result = OutboxDlqRepositoryFactory.generate(dataSource);

        // then
        assertInstanceOf(OracleOutboxDlqRepository.class, result);
    }

    @Test
    @DisplayName("UT generate() when unsupported DB should throw IAE")
    void generate_whenUnsupportedDb_shouldThrowIAE() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");

        // when + then
        assertThrows(IllegalArgumentException.class, () -> OutboxDlqRepositoryFactory.generate(dataSource));
    }

    @Test
    @DisplayName("UT generate() when SQLException should throw RuntimeException")
    void generate_whenSqlException_shouldThrowRuntimeException() throws SQLException {
        // given
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // when + then
        assertThrows(RuntimeException.class, () -> OutboxDlqRepositoryFactory.generate(dataSource));
    }
}
