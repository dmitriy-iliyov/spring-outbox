package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.starter.consumer.OutboxConsumerProperties;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxPublisherProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.MetaDataAccessException;

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

    private void mockDbProductName(String productName) throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn(productName);
    }

    @Test
    @DisplayName("UT create() when properties is null should throw NullPointerException")
    void create_whenPropertiesNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> OutboxDatabasePopulatorFactory.create(null, dataSource))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("properties cannot be null");
    }

    @Test
    @DisplayName("UT create() when dataSource is null should throw NullPointerException")
    void create_whenDataSourceNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> OutboxDatabasePopulatorFactory.create(properties, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("dataSource cannot be null");
    }

    @Test
    @DisplayName("UT create() when PostgreSQL and only base tables enabled should return populator")
    void create_whenPostgreSqlAndBaseTables_shouldReturnPopulator() throws SQLException {
        mockDbProductName("PostgreSQL");
        when(properties.getPublisher()).thenReturn(publisherProperties);
        when(publisherProperties.getDlq()).thenReturn(null);
        when(properties.getConsumer()).thenReturn(null);

        DatabasePopulator result = OutboxDatabasePopulatorFactory.create(properties, dataSource);

        assertThat(result).isNotNull().isInstanceOf(ResourceDatabasePopulator.class);
        verify(connection).close();
    }

    @Test
    @DisplayName("UT create() when PostgreSQL with DLQ enabled should return populator")
    void create_whenPostgreSqlWithDlqEnabled_shouldReturnPopulator() throws SQLException {
        mockDbProductName("PostgreSQL");
        when(properties.getPublisher()).thenReturn(publisherProperties);
        when(publisherProperties.getDlq()).thenReturn(dlqProperties);
        when(dlqProperties.isEnabled()).thenReturn(true);
        when(properties.getConsumer()).thenReturn(null);

        DatabasePopulator result = OutboxDatabasePopulatorFactory.create(properties, dataSource);

        assertThat(result).isNotNull().isInstanceOf(ResourceDatabasePopulator.class);
    }

    @Test
    @DisplayName("UT create() when PostgreSQL with consumer enabled should return populator")
    void create_whenPostgreSqlWithConsumerEnabled_shouldReturnPopulator() throws SQLException {
        mockDbProductName("PostgreSQL");
        when(properties.getPublisher()).thenReturn(publisherProperties);
        when(publisherProperties.getDlq()).thenReturn(null);
        when(properties.getConsumer()).thenReturn(consumerProperties);
        when(consumerProperties.isEnabled()).thenReturn(true);

        DatabasePopulator result = OutboxDatabasePopulatorFactory.create(properties, dataSource);

        assertThat(result).isNotNull().isInstanceOf(ResourceDatabasePopulator.class);
    }

    @Test
    @DisplayName("UT create() when PostgreSQL with DLQ and consumer enabled should return populator")
    void create_whenPostgreSqlWithDlqAndConsumerEnabled_shouldReturnPopulator() throws SQLException {
        mockDbProductName("PostgreSQL");
        when(properties.getPublisher()).thenReturn(publisherProperties);
        when(publisherProperties.getDlq()).thenReturn(dlqProperties);
        when(dlqProperties.isEnabled()).thenReturn(true);
        when(properties.getConsumer()).thenReturn(consumerProperties);
        when(consumerProperties.isEnabled()).thenReturn(true);

        DatabasePopulator result = OutboxDatabasePopulatorFactory.create(properties, dataSource);

        assertThat(result).isNotNull().isInstanceOf(ResourceDatabasePopulator.class);
    }

    @Test
    @DisplayName("UT create() when MySQL with DLQ and consumer enabled should return populator")
    void create_whenMySqlWithDlqAndConsumerEnabled_shouldReturnPopulator() throws SQLException {
        mockDbProductName("MySQL");
        when(properties.getPublisher()).thenReturn(publisherProperties);
        when(publisherProperties.getDlq()).thenReturn(dlqProperties);
        when(dlqProperties.isEnabled()).thenReturn(true);
        when(properties.getConsumer()).thenReturn(consumerProperties);
        when(consumerProperties.isEnabled()).thenReturn(true);

        DatabasePopulator result = OutboxDatabasePopulatorFactory.create(properties, dataSource);

        assertThat(result).isNotNull().isInstanceOf(ResourceDatabasePopulator.class);
    }

    @Test
    @DisplayName("UT create() when Oracle with DLQ and consumer enabled should return populator")
    void create_whenOracleWithDlqAndConsumerEnabled_shouldReturnPopulator() throws SQLException {
        mockDbProductName("Oracle");
        when(properties.getPublisher()).thenReturn(publisherProperties);
        when(publisherProperties.getDlq()).thenReturn(dlqProperties);
        when(dlqProperties.isEnabled()).thenReturn(true);
        when(properties.getConsumer()).thenReturn(consumerProperties);
        when(consumerProperties.isEnabled()).thenReturn(true);

        DatabasePopulator result = OutboxDatabasePopulatorFactory.create(properties, dataSource);

        assertThat(result).isNotNull().isInstanceOf(ResourceDatabasePopulator.class);
    }

    @Test
    @DisplayName("UT create() when DLQ and consumer explicitly disabled should return base populator")
    void create_whenDlqAndConsumerExplicitlyDisabled_shouldReturnBasePopulator() throws SQLException {
        mockDbProductName("PostgreSQL");
        when(properties.getPublisher()).thenReturn(publisherProperties);
        when(publisherProperties.getDlq()).thenReturn(dlqProperties);
        when(dlqProperties.isEnabled()).thenReturn(false);
        when(properties.getConsumer()).thenReturn(consumerProperties);
        when(consumerProperties.isEnabled()).thenReturn(false);

        DatabasePopulator result = OutboxDatabasePopulatorFactory.create(properties, dataSource);

        assertThat(result).isNotNull().isInstanceOf(ResourceDatabasePopulator.class);
    }

    @Test
    @DisplayName("UT create() when connection fails should throw RuntimeException")
    void create_whenConnectionFails_shouldThrowRuntimeException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        assertThatThrownBy(() -> OutboxDatabasePopulatorFactory.create(properties, dataSource))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(MetaDataAccessException.class);
    }

    @Test
    @DisplayName("UT create() when metadata extraction fails should throw RuntimeException")
    void create_whenMetadataExtractionFails_shouldThrowRuntimeException() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenThrow(new SQLException("Metadata failed"));

        assertThatThrownBy(() -> OutboxDatabasePopulatorFactory.create(properties, dataSource))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(MetaDataAccessException.class);
    }
}
