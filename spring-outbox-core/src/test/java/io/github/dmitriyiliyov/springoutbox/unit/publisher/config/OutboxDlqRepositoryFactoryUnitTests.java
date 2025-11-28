package io.github.dmitriyiliyov.springoutbox.unit.publisher.config;

import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxDlqRepositoryFactory;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.MySqlOutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.OracleOutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.PostgreSqlOutboxDlqRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OutboxDlqRepositoryFactoryUnitTests {

    @Test
    @DisplayName("UT generate() with PostgreSQL should return PostgreSqlOutboxDlqRepository")
    public void generate_postgresql_shouldReturnPostgreSqlRepository() throws Exception {
        // given
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        // when
        OutboxDlqRepository repository = OutboxDlqRepositoryFactory.generate(dataSource);

        // then
        assertNotNull(repository);
        assertTrue(repository instanceof PostgreSqlOutboxDlqRepository);
        verify(connection).close();
    }

    @Test
    @DisplayName("UT generate() with MySql should return MySqlOutboxDlqRepository")
    public void generate_postgresql_shouldReturnMySqlRepository() throws Exception {
        // given
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySql");

        // when
        OutboxDlqRepository repository = OutboxDlqRepositoryFactory.generate(dataSource);

        // then
        assertNotNull(repository);
        assertTrue(repository instanceof MySqlOutboxDlqRepository);
        verify(connection).close();
    }

    @Test
    @DisplayName("UT generate() with Oracle should return OracleOutboxDlqRepository")
    public void generate_postgresql_shouldReturnOracleRepository() throws Exception {
        // given
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Oracle");

        // when
        OutboxDlqRepository repository = OutboxDlqRepositoryFactory.generate(dataSource);

        // then
        assertNotNull(repository);
        assertTrue(repository instanceof OracleOutboxDlqRepository);
        verify(connection).close();
    }

    @Test
    @DisplayName("UT generate() with unsupported DB should throw IllegalArgumentException")
    public void generate_unsupportedDb_shouldThrow() throws Exception {
        // given
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("nonExistsDb");

        // when + then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> OutboxDlqRepositoryFactory.generate(dataSource));

        assertTrue(ex.getMessage().contains("Unsupported database 'nonExistsDb'"));
        verify(connection).close();
    }

    @Test
    @DisplayName("UT generate() when DataSource.getConnection() throws SQLException should throw RuntimeException")
    public void generate_connectionThrowsSQLException_shouldThrowRuntimeException() throws Exception {
        // given
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("DB not reachable"));

        // when + then
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> OutboxDlqRepositoryFactory.generate(dataSource));

        assertTrue(ex.getCause() instanceof SQLException);
    }
}
