package io.github.dmitriyiliyov.springoutbox.unit.config;

import io.github.dmitriyiliyov.springoutbox.config.OutboxRepositoryFactory;
import io.github.dmitriyiliyov.springoutbox.core.OutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.PostgreSqlOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OutboxRepositoryFactoryUnitTests {

    @Test
    @DisplayName("UT generate() with PostgreSQL should return PostgreSqlOutboxRepository")
    public void generate_postgresql_shouldReturnPostgreSqlRepository() throws Exception {
        // given
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        // when
        OutboxRepository repository = OutboxRepositoryFactory.generate(dataSource);

        // then
        assertNotNull(repository);
        assertTrue(repository instanceof PostgreSqlOutboxRepository);
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
        when(metaData.getDatabaseProductName()).thenReturn("Oracle");

        // when + then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> OutboxRepositoryFactory.generate(dataSource));

        assertTrue(ex.getMessage().contains("Supplier for OutboxRepository is null for databaseType=ORACLE"));
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
                () -> OutboxRepositoryFactory.generate(dataSource));

        assertTrue(ex.getCause() instanceof SQLException);
    }
}
