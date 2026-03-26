package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.starter.it.OracleTestContainerSingleton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OracleOutboxAutoConfigurationIntegrationTests {

    private final OutboxAutoConfigurationVerifier verifier =
            new OutboxAutoConfigurationVerifier(
                    OracleTestContainerSingleton.INSTANCE.getJdbcUrl(),
                    "oracle.jdbc.OracleDriver",
                    OracleTestContainerSingleton.INSTANCE.getUsername(),
                    OracleTestContainerSingleton.INSTANCE.getPassword()
            );

    @Test
    @DisplayName("IT should register OutboxPublisherProperties bean")
    void shouldRegisterOutboxPublisherPropertiesBean() {
        verifier.shouldRegisterOutboxPublisherPropertiesBean();
    }

    @Test
    @DisplayName("IT should register OutboxConsumerProperties bean")
    void shouldRegisterOutboxConsumerPropertiesBean() {
        verifier.shouldRegisterOutboxConsumerPropertiesBean();
    }

    @Test
    @DisplayName("IT should register transaction-aware JdbcTemplate bean")
    void shouldRegisterTransactionAwareJdbcTemplate() {
        verifier.shouldRegisterTransactionAwareJdbcTemplate();
    }

    @Test
    @DisplayName("IT should register ScheduledExecutorService bean")
    void shouldRegisterScheduledExecutorServiceBean() {
        verifier.shouldRegisterScheduledExecutorServiceBean();
    }

    @Test
    @DisplayName("IT should register PostApplicationStartOutboxInitializer bean when none present")
    void shouldRegisterOutboxInitializerWhenMissing() {
        verifier.shouldRegisterOutboxInitializerWhenMissing();
    }

    @Test
    @DisplayName("IT should not register PostApplicationStartOutboxInitializer when bean already present")
    void shouldNotRegisterOutboxInitializerWhenAlreadyPresent() {
        verifier.shouldNotRegisterOutboxInitializerWhenAlreadyPresent();
    }

    @Test
    @DisplayName("IT should not register DataSourceInitializer when auto-create is false")
    void shouldNotRegisterDataSourceInitializerWhenAutoCreateFalse() {
        verifier.shouldNotRegisterDataSourceInitializerWhenAutoCreateFalse();
    }

    @Test
    @DisplayName("IT should create outbox_events table when auto-create is true")
    void shouldCreateTablesWhenAutoCreateTrue() {
        verifier.shouldCreateTablesWhenAutoCreateTrue();
    }

    @Test
    @DisplayName("IT JdbcTemplate bean should be transaction-aware")
    void jdbcTemplateShouldBeTransactionAware() {
        verifier.jdbcTemplateShouldBeTransactionAware();
    }

    @Test
    @DisplayName("IT publisher bean should have enabled=false when publisher not configured")
    void publisherBeanShouldBeDisabledByDefault() {
        verifier.publisherBeanShouldBeDisabledByDefault();
    }

    @Test
    @DisplayName("IT consumer bean should have enabled=false when consumer not configured")
    void consumerBeanShouldBeDisabledByDefault() {
        verifier.consumerBeanShouldBeDisabledByDefault();
    }

    @Test
    @DisplayName("IT should fail to start context when DataSource is not available")
    void shouldFailWhenDataSourceNotAvailable() {
        verifier.shouldFailWhenDataSourceNotAvailable();
    }

    @Test
    @DisplayName("IT ScheduledExecutorService should use default thread pool size when not configured")
    void shouldUseDefaultThreadPoolSizeWhenNotConfigured() {
        verifier.shouldUseDefaultThreadPoolSizeWhenNotConfigured();
    }
}
