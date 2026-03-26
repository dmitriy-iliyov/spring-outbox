package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.starter.it.OracleTestContainerSingleton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class OracleOutboxConsumerAutoConfigurationIntegrationTests {


    private final OutboxConsumerAutoConfigurationVerifier verifier =
            new OutboxConsumerAutoConfigurationVerifier(
                    OracleTestContainerSingleton.INSTANCE.getJdbcUrl(),
                    "oracle.jdbc.OracleDriver",
                    OracleTestContainerSingleton.INSTANCE.getUsername(),
                    OracleTestContainerSingleton.INSTANCE.getPassword()
            );

    @Test
    @DisplayName("IT should not load consumer config when enabled=false")
    void shouldNotLoadWhenDisabled() {
        verifier.shouldNotLoadWhenDisabled();
    }

    @Test
    @DisplayName("IT should register ConsumedOutboxRepository bean")
    void shouldRegisterConsumedOutboxRepository() {
        verifier.shouldRegisterConsumedOutboxRepository();
    }

    @Test
    @DisplayName("IT should register ConsumedOutboxManager bean")
    void shouldRegisterConsumedOutboxManager() {
        verifier.shouldRegisterConsumedOutboxManager();
    }

    @Test
    @DisplayName("IT should register OutboxIdempotentConsumer bean")
    void shouldRegisterOutboxIdempotentConsumer() {
        verifier.shouldRegisterOutboxIdempotentConsumer();
    }

    @Test
    @DisplayName("IT should register OutboxEventIdResolveManager bean")
    void shouldRegisterOutboxEventIdResolveManager() {
        verifier.shouldRegisterOutboxEventIdResolveManager();
    }

    @Test
    @DisplayName("IT should register Kafka OutboxEventIdResolver when kafka is on classpath")
    void shouldRegisterKafkaEventIdResolver() {
        verifier.shouldRegisterKafkaEventIdResolver();
    }

    @Test
    @DisplayName("IT should register RabbitMq OutboxEventIdResolver when rabbit is on classpath")
    void shouldRegisterRabbitMqEventIdResolver() {
        verifier.shouldRegisterRabbitMqEventIdResolver();
    }

    @Test
    @DisplayName("IT should not register ConsumedOutboxCleanUpScheduler when clean-up disabled")
    void shouldNotRegisterCleanUpSchedulerWhenDisabled() {
        verifier.shouldNotRegisterCleanUpSchedulerWhenDisabled();
    }

    @Test
    @DisplayName("IT should not register duplicate ConsumedOutboxManager when custom bean provided")
    void shouldNotRegisterDuplicateConsumedOutboxManagerWhenCustomBeanProvided() {
        verifier.shouldNotRegisterDuplicateConsumedOutboxManagerWhenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should create outbox_consumed_events table when auto-create is true")
    void shouldCreateTablesWhenAutoCreateTrue() {
        verifier.shouldCreateTablesWhenAutoCreateTrue();
    }
}
