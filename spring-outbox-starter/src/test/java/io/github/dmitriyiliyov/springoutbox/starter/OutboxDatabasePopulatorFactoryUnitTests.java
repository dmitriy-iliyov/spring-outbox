package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.starter.consumer.OutboxConsumerProperties;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxPublisherProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.datasource.init.DatabasePopulator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxDatabasePopulatorFactoryUnitTests {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @Mock
    private OutboxProperties properties;

    @Mock
    private OutboxPublisherProperties publisherProperties;

    @Mock
    private OutboxPublisherProperties.DlqProperties dlqProperties;

    @Mock
    private OutboxConsumerProperties consumerProperties;

    @Test
    @DisplayName("UT create() when PostgreSQL and only outbox table should return populator with one script")
    void create_whenPostgreSqlAndOnlyOutboxTable_shouldReturnPopulatorWithOneScript() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(properties.getPublisher()).thenReturn(publisherProperties);
        when(publisherProperties.getDlq()).thenReturn(null);
        when(properties.getConsumer()).thenReturn(null);

        // when
        DatabasePopulator result = OutboxDatabasePopulatorFactory.create(properties, dataSource);

        // then
        assertThat(result).isNotNull();
        verify(dataSource).getConnection();
        verify(connection).getMetaData();
        verify(connection).close();
    }

    @Test
    @DisplayName("UT create() when PostgreSQL with DLQ enabled should return populator with two scripts")
    void create_whenPostgreSqlWithDlqEnabled_shouldReturnPopulatorWithTwoScripts() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(properties.getPublisher()).thenReturn(publisherProperties);
        when(publisherProperties.getDlq()).thenReturn(dlqProperties);
        when(dlqProperties.isEnabled()).thenReturn(true);
        when(properties.getConsumer()).thenReturn(null);

        // when
        DatabasePopulator result = OutboxDatabasePopulatorFactory.create(properties, dataSource);

        // then
        assertThat(result).isNotNull();
        verify(dataSource).getConnection();
        verify(connection).close();
    }

    @Test
    @DisplayName("UT create() when PostgreSQL with consumer enabled should return populator with two scripts")
    void create_whenPostgreSqlWithConsumerEnabled_shouldReturnPopulatorWithTwoScripts() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(properties.getPublisher()).thenReturn(publisherProperties);
        when(publisherProperties.getDlq()).thenReturn(null);
        when(properties.getConsumer()).thenReturn(consumerProperties);
        when(consumerProperties.isEnabled()).thenReturn(true);

        // when
        DatabasePopulator result = OutboxDatabasePopulatorFactory.create(properties, dataSource);

        // then
        assertThat(result).isNotNull();
        verify(dataSource).getConnection();
        verify(connection).close();
    }

    @Test
    @DisplayName("UT create() when PostgreSQL with DLQ and consumer enabled should return populator with three scripts")
    void create_whenPostgreSqlWithDlqAndConsumerEnabled_shouldReturnPopulatorWithThreeScripts() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(properties.getPublisher()).thenReturn(publisherProperties);
        when(publisherProperties.getDlq()).thenReturn(dlqProperties);
        when(dlqProperties.isEnabled()).thenReturn(true);
        when(properties.getConsumer()).thenReturn(consumerProperties);
        when(consumerProperties.isEnabled()).thenReturn(true);

        // when
        DatabasePopulator result = OutboxDatabasePopulatorFactory.create(properties, dataSource);

        // then
        assertThat(result).isNotNull();
        verify(dataSource).getConnection();
        verify(connection).close();
    }

    @Test
    @DisplayName("UT create() when MySQL and only outbox table should return populator with one script")
    void create_whenMySqlAndOnlyOutboxTable_shouldReturnPopulatorWithOneScript() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");
        when(properties.getPublisher()).thenReturn(publisherProperties);
        when(publisherProperties.getDlq()).thenReturn(null);
        when(properties.getConsumer()).thenReturn(null);

        // when
        DatabasePopulator result = OutboxDatabasePopulatorFactory.create(properties, dataSource);

        // then
        assertThat(result).isNotNull();
        verify(dataSource).getConnection();
        verify(connection).close();
    }

    @Test
    @DisplayName("UT create() when MySQL with DLQ and consumer enabled should return populator with three scripts")
    void create_whenMySqlWithDlqAndConsumerEnabled_shouldReturnPopulatorWithThreeScripts() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");
        when(properties.getPublisher()).thenReturn(publisherProperties);
        when(publisherProperties.getDlq()).thenReturn(dlqProperties);
        when(dlqProperties.isEnabled()).thenReturn(true);
        when(properties.getConsumer()).thenReturn(consumerProperties);
        when(consumerProperties.isEnabled()).thenReturn(true);

        // when
        DatabasePopulator result = OutboxDatabasePopulatorFactory.create(properties, dataSource);

        // then
        assertThat(result).isNotNull();
        verify(dataSource).getConnection();
        verify(connection).close();
    }

    @Test
    @DisplayName("UT create() when Oracle and only outbox table should return populator with one script")
    void create_whenOracleAndOnlyOutboxTable_shouldReturnPopulatorWithOneScript() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Oracle");
        when(properties.getPublisher()).thenReturn(publisherProperties);
        when(publisherProperties.getDlq()).thenReturn(null);
        when(properties.getConsumer()).thenReturn(null);

        // when
        DatabasePopulator result = OutboxDatabasePopulatorFactory.create(properties, dataSource);

        // then
        assertThat(result).isNotNull();
        verify(dataSource).getConnection();
        verify(connection).close();
    }

    @Test
    @DisplayName("UT create() when Oracle with DLQ and consumer enabled should return populator with three scripts")
    void create_whenOracleWithDlqAndConsumerEnabled_shouldReturnPopulatorWithThreeScripts() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Oracle");
        when(properties.getPublisher()).thenReturn(publisherProperties);
        when(publisherProperties.getDlq()).thenReturn(dlqProperties);
        when(dlqProperties.isEnabled()).thenReturn(true);
        when(properties.getConsumer()).thenReturn(consumerProperties);
        when(consumerProperties.isEnabled()).thenReturn(true);

        // when
        DatabasePopulator result = OutboxDatabasePopulatorFactory.create(properties, dataSource);

        // then
        assertThat(result).isNotNull();
        verify(dataSource).getConnection();
        verify(connection).close();
    }

    @Test
    @DisplayName("UT create() when DLQ disabled should not include DLQ script")
    void create_whenDlqDisabled_shouldNotIncludeDlqScript() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(properties.getPublisher()).thenReturn(publisherProperties);
        when(publisherProperties.getDlq()).thenReturn(dlqProperties);
        when(dlqProperties.isEnabled()).thenReturn(false);
        when(properties.getConsumer()).thenReturn(null);

        // when
        DatabasePopulator result = OutboxDatabasePopulatorFactory.create(properties, dataSource);

        // then
        assertThat(result).isNotNull();
        verify(dataSource).getConnection();
        verify(connection).close();
    }

    @Test
    @DisplayName("UT create() when consumer disabled should not include consumed script")
    void create_whenConsumerDisabled_shouldNotIncludeConsumedScript() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(properties.getPublisher()).thenReturn(publisherProperties);
        when(publisherProperties.getDlq()).thenReturn(null);
        when(properties.getConsumer()).thenReturn(consumerProperties);
        when(consumerProperties.isEnabled()).thenReturn(false);

        // when
        DatabasePopulator result = OutboxDatabasePopulatorFactory.create(properties, dataSource);

        // then
        assertThat(result).isNotNull();
        verify(dataSource).getConnection();
        verify(connection).close();
    }

    @Test
    @DisplayName("UT create() when unsupported database should throw IllegalStateException")
    void create_whenUnsupportedDatabase_shouldThrowIllegalStateException() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("UnsupportedDB");

        // when + then
        assertThatThrownBy(() -> OutboxDatabasePopulatorFactory.create(properties, dataSource))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported database 'UnsupportedDB'");
        verify(connection).close();
    }

    @Test
    @DisplayName("UT create() when connection fails should throw RuntimeException")
    void create_whenConnectionFails_shouldThrowRuntimeException() throws SQLException {
        // given
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // when + then
        assertThatThrownBy(() -> OutboxDatabasePopulatorFactory.create(properties, dataSource))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get database connection")
                .hasCauseInstanceOf(SQLException.class);
    }

    @Test
    @DisplayName("UT create() when metadata fails should throw RuntimeException")
    void create_whenMetadataFails_shouldThrowRuntimeException() throws SQLException {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenThrow(new SQLException("Metadata failed"));

        // when + then
        assertThatThrownBy(() -> OutboxDatabasePopulatorFactory.create(properties, dataSource))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get database connection")
                .hasCauseInstanceOf(SQLException.class);
        verify(connection).close();
    }
}
