package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.consumer.MySqlConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.consumer.OracleConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.consumer.PostgreSqlConsumedOutboxRepository;
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
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumedOutboxRepositoryFactoryUnitTests {

    @Mock
    DataSource dataSource;

    @Mock
    JdbcTemplate jdbcTemplate;

    @Mock
    Clock clock;

    @Mock
    Connection connection;

    @Mock
    DatabaseMetaData metaData;

    @Test
    @DisplayName("UT create() when database is postgresql should return PostgreSqlConsumedOutboxRepository")
    void create_whenDatabaseIsPostgresql_shouldReturnPostgresRepository() throws Exception {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        // when
        ConsumedOutboxRepository repository = ConsumedOutboxRepositoryFactory.create(dataSource, jdbcTemplate, clock);

        // then
        assertThat(repository).isInstanceOf(PostgreSqlConsumedOutboxRepository.class);
        verify(dataSource, times(1)).getConnection();
        verify(connection, times(1)).getMetaData();
        verify(metaData, times(1)).getDatabaseProductName();
        verify(connection, times(1)).close();
        verifyNoMoreInteractions(dataSource, connection, metaData);
    }

    @Test
    @DisplayName("UT create() when database is mysql should return MySqlConsumedOutboxRepository")
    void create_whenDatabaseIsMysql_shouldReturnMySqlRepository() throws Exception {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");

        // when
        ConsumedOutboxRepository repository = ConsumedOutboxRepositoryFactory.create(dataSource, jdbcTemplate, clock);

        // then
        assertThat(repository).isInstanceOf(MySqlConsumedOutboxRepository.class);
        verify(dataSource, times(1)).getConnection();
        verify(connection, times(1)).getMetaData();
        verify(metaData, times(1)).getDatabaseProductName();
        verify(connection, times(1)).close();
        verifyNoMoreInteractions(dataSource, connection, metaData);
    }

    @Test
    @DisplayName("UT create() when database is oracle should return OracleConsumedOutboxRepository")
    void create_whenDatabaseIsOracle_shouldReturnOracleRepository() throws Exception {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Oracle");

        // when
        ConsumedOutboxRepository repository = ConsumedOutboxRepositoryFactory.create(dataSource, jdbcTemplate, clock);

        // then
        assertThat(repository).isInstanceOf(OracleConsumedOutboxRepository.class);
        verify(dataSource, times(1)).getConnection();
        verify(connection, times(1)).getMetaData();
        verify(metaData, times(1)).getDatabaseProductName();
        verify(connection, times(1)).close();
        verifyNoMoreInteractions(dataSource, connection, metaData);
    }

    @Test
    @DisplayName("UT create() when database is unsupported should throw IllegalArgumentException")
    void create_whenDatabaseIsUnsupported_shouldThrow() throws Exception {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("UnsupportedDB");

        // when + then
        assertThrows(IllegalArgumentException.class, () ->
                ConsumedOutboxRepositoryFactory.create(dataSource, jdbcTemplate, clock)
        );

        verify(dataSource, times(1)).getConnection();
        verify(connection, times(1)).getMetaData();
        verify(metaData, times(1)).getDatabaseProductName();
        verify(connection, times(1)).close();
        verifyNoMoreInteractions(dataSource, connection, metaData);
    }

    @Test
    @DisplayName("UT create() when dataSource is null should throw NPE")
    void create_whenDataSourceIsNull_shouldThrowNPE() {
        assertThrows(NullPointerException.class, () -> ConsumedOutboxRepositoryFactory.create(null, jdbcTemplate, clock));
    }

    @Test
    @DisplayName("UT create() when jdbcTemplate is null should throw IllegalStateException")
    void create_whenJdbcTemplateIsNull_shouldThrowIllegalStateException() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        // when + then
        assertThrows(IllegalStateException.class, () -> ConsumedOutboxRepositoryFactory.create(dataSource, null, clock));
    }

    @Test
    @DisplayName("UT create() when clock is null should throw IllegalStateException")
    void create_whenClockIsNull_shouldThrowIllegalStateException() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

        // when + then
        assertThrows(IllegalStateException.class, () -> ConsumedOutboxRepositoryFactory.create(dataSource, jdbcTemplate, null));
    }

    @Test
    @DisplayName("UT create() when dataSource and jdbcTemplate is null should throw NPE")
    void create_whenDataSourceAndJdbcTemplateIsNull_shouldThrowNPE() {
        assertThrows(NullPointerException.class, () -> ConsumedOutboxRepositoryFactory.create(null, null, null));
    }

    @Test
    @DisplayName("UT create() when connection throws should throw Exception")
    void create_whenConnectionThrows_shouldThrowException() throws Exception {
        // given
        when(dataSource.getConnection()).thenThrow(new SQLException());

        // when + then
        assertThrows(RuntimeException.class, () -> ConsumedOutboxRepositoryFactory.create(dataSource, jdbcTemplate, clock));
        verify(dataSource, times(1)).getConnection();
        verifyNoMoreInteractions(dataSource);
    }

    @Test
    @DisplayName("UT create() when connection throws should throw RuntimeException")
    void create_whenConnectionThrows_shouldThrowRuntimeException() throws Exception {
        // given
        when(dataSource.getConnection()).thenThrow(new RuntimeException());

        // when + then
        assertThrows(RuntimeException.class, () -> ConsumedOutboxRepositoryFactory.create(dataSource, jdbcTemplate, clock));
        verify(dataSource, times(1)).getConnection();
        verifyNoMoreInteractions(dataSource);
    }
}
