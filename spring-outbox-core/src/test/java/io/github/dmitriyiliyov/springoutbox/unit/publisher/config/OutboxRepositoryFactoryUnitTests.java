package io.github.dmitriyiliyov.springoutbox.unit.publisher.config;

import io.github.dmitriyiliyov.springoutbox.publisher.MySqlOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.publisher.OracleOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.publisher.OutboxRepository;
import io.github.dmitriyiliyov.springoutbox.publisher.PostgreSqlOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxRepositoryFactory;
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
class OutboxRepositoryFactoryUnitTests {

    @Mock
    DataSource dataSource;

    @Mock
    Connection connection;

    @Mock
    DatabaseMetaData metaData;

    @Test
    @DisplayName("UT generate() when PostgreSQL should return PostgreSqlOutboxRepository")
    void generate_whenPostgres_shouldReturnPostgreSqlRepository() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        // when
        OutboxRepository result = OutboxRepositoryFactory.generate(dataSource);

        // then
        assertInstanceOf(PostgreSqlOutboxRepository.class, result);
    }

    @Test
    @DisplayName("UT generate() when MySQL should return MySqlOutboxRepository")
    void generate_whenMySql_shouldReturnMySqlRepository() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");

        // when
        OutboxRepository result = OutboxRepositoryFactory.generate(dataSource);

        // then
        assertInstanceOf(MySqlOutboxRepository.class, result);
    }

    @Test
    @DisplayName("UT generate() when Oracle should return OracleOutboxRepository")
    void generate_whenOracle_shouldReturnOracleRepository() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Oracle");

        // when
        OutboxRepository result = OutboxRepositoryFactory.generate(dataSource);

        // then
        assertInstanceOf(OracleOutboxRepository.class, result);
    }

    @Test
    @DisplayName("UT generate() when unsupported DB should throw IAE")
    void generate_whenUnsupportedDb_shouldThrowIAE() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");

        // when + then
        assertThrows(IllegalArgumentException.class, () -> OutboxRepositoryFactory.generate(dataSource));
    }

    @Test
    @DisplayName("UT generate() when SQLException should throw RuntimeException")
    void generate_whenSqlException_shouldThrowRuntimeException() throws SQLException {
        // given
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // when + then
        assertThrows(RuntimeException.class, () -> OutboxRepositoryFactory.generate(dataSource));
    }
}
