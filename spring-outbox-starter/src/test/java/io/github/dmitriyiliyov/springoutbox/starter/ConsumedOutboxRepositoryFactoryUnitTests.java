package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.consumer.MySqlConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.consumer.OracleConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.consumer.PostgreSqlConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.starter.consumer.ConsumedOutboxRepositoryFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumedOutboxRepositoryFactoryUnitTests {

    @Mock
    DataSource dataSource;

    @Mock
    Connection connection;

    @Mock
    DatabaseMetaData metaData;

    @Test
    @DisplayName("UT generate() when database is postgresql should return PostgreSqlConsumedOutboxRepository")
    void generate_whenDatabaseIsPostgresql_shouldReturnPostgresRepository() throws Exception {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        // when
        ConsumedOutboxRepository repository = ConsumedOutboxRepositoryFactory.generate(dataSource);

        // then
        assertThat(repository).isInstanceOf(PostgreSqlConsumedOutboxRepository.class);
        verify(dataSource, times(1)).getConnection();
        verify(connection, times(1)).getMetaData();
        verify(metaData, times(1)).getDatabaseProductName();
        verify(connection, times(1)).close();
        verifyNoMoreInteractions(dataSource, connection, metaData);
    }

    @Test
    @DisplayName("UT generate() when database is mysql should return MySqlConsumedOutboxRepository")
    void generate_whenDatabaseIsMysql_shouldReturnMySqlRepository() throws Exception {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");

        // when
        ConsumedOutboxRepository repository = ConsumedOutboxRepositoryFactory.generate(dataSource);

        // then
        assertThat(repository).isInstanceOf(MySqlConsumedOutboxRepository.class);
        verify(dataSource, times(1)).getConnection();
        verify(connection, times(1)).getMetaData();
        verify(metaData, times(1)).getDatabaseProductName();
        verify(connection, times(1)).close();
        verifyNoMoreInteractions(dataSource, connection, metaData);
    }

    @Test
    @DisplayName("UT generate() when database is oracle should return OracleConsumedOutboxRepository")
    void generate_whenDatabaseIsOracle_shouldReturnOracleRepository() throws Exception {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Oracle");

        // when
        ConsumedOutboxRepository repository = ConsumedOutboxRepositoryFactory.generate(dataSource);

        // then
        assertThat(repository).isInstanceOf(OracleConsumedOutboxRepository.class);
        verify(dataSource, times(1)).getConnection();
        verify(connection, times(1)).getMetaData();
        verify(metaData, times(1)).getDatabaseProductName();
        verify(connection, times(1)).close();
        verifyNoMoreInteractions(dataSource, connection, metaData);
    }

    @Test
    @DisplayName("UT generate() when database is unsupported should throw IllegalArgumentException")
    void generate_whenDatabaseIsUnsupported_shouldThrow() throws Exception {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("UnsupportedDB");

        // when + then
        assertThrows(IllegalArgumentException.class, () ->
                ConsumedOutboxRepositoryFactory.generate(dataSource)
        );

        verify(dataSource, times(1)).getConnection();
        verify(connection, times(1)).getMetaData();
        verify(metaData, times(1)).getDatabaseProductName();
        verify(connection, times(1)).close();
        verifyNoMoreInteractions(dataSource, connection, metaData);
    }

    @Test
    @DisplayName("UT generate() when dataSource is null should throw NPE")
    void generate_whenDataSourceIsNull_shouldThrowNPE() {
        assertThrows(NullPointerException.class, () -> ConsumedOutboxRepositoryFactory.generate(null));
    }

    @Test
    @DisplayName("UT generate() when connection throws should throw Exception")
    void generate_whenConnectionThrows_shouldThrowException() throws Exception {
        // given
        when(dataSource.getConnection()).thenThrow(new SQLException());

        // when + then
        assertThrows(RuntimeException.class, () -> ConsumedOutboxRepositoryFactory.generate(dataSource));
        verify(dataSource, times(1)).getConnection();
        verifyNoMoreInteractions(dataSource);
    }

    @Test
    @DisplayName("UT generate() when connection throws should throw RuntimeException")
    void generate_whenConnectionThrows_shouldThrowRuntimeException() throws Exception {
        // given
        when(dataSource.getConnection()).thenThrow(new RuntimeException());

        // when + then
        assertThrows(RuntimeException.class, () -> ConsumedOutboxRepositoryFactory.generate(dataSource));
        verify(dataSource, times(1)).getConnection();
        verifyNoMoreInteractions(dataSource);
    }
}
