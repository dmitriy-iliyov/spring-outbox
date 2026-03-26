package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.starter.it.MySqlTestContainerSingleton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MySqlOutboxPublisherAutoConfigurationIntegrationTests {

    private final OutboxPublisherAutoConfigurationVerifier verifier =
            new OutboxPublisherAutoConfigurationVerifier(
                    MySqlTestContainerSingleton.INSTANCE.getJdbcUrl(),
                    "com.mysql.cj.jdbc.Driver",
                    MySqlTestContainerSingleton.INSTANCE.getUsername(),
                    MySqlTestContainerSingleton.INSTANCE.getPassword()
            );

    @Test
    @DisplayName("IT should not load publisher config when enabled=false")
    void shouldNotLoadWhenDisabled() {
        verifier.shouldNotLoadWhenDisabled();
    }

    @Test
    @DisplayName("IT should register OutboxRepository bean")
    void shouldRegisterOutboxRepository() {
        verifier.shouldRegisterOutboxRepository();
    }

    @Test
    @DisplayName("IT should register OutboxManager bean")
    void shouldRegisterOutboxManager() {
        verifier.shouldRegisterOutboxManager();
    }

    @Test
    @DisplayName("IT should register OutboxProcessor bean")
    void shouldRegisterOutboxProcessor() {
        verifier.shouldRegisterOutboxProcessor();
    }

    @Test
    @DisplayName("IT should register OutboxSerializer bean")
    void shouldRegisterOutboxSerializer() {
        verifier.shouldRegisterOutboxSerializer();
    }

    @Test
    @DisplayName("IT should register OutboxPublisher bean")
    void shouldRegisterOutboxPublisher() {
        verifier.shouldRegisterOutboxPublisher();
    }

    @Test
    @DisplayName("IT should register UuidGenerator bean")
    void shouldRegisterUuidGenerator() {
        verifier.shouldRegisterUuidGenerator();
    }

    @Test
    @DisplayName("IT should register OutboxPublishAspect bean")
    void shouldRegisterOutboxPublishAspect() {
        verifier.shouldRegisterOutboxPublishAspect();
    }

    @Test
    @DisplayName("IT should register RowOutboxEventListener bean")
    void shouldRegisterRowOutboxEventListener() {
        verifier.shouldRegisterRowOutboxEventListener();
    }

    @Test
    @DisplayName("IT should not register duplicate OutboxRepository when custom bean provided")
    void shouldNotRegisterOutboxRepositoryWhenCustomBeanProvided() {
        verifier.shouldNotRegisterOutboxRepositoryWhenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should not register duplicate OutboxProcessor when custom bean provided")
    void shouldNotRegisterOutboxProcessorWhenCustomBeanProvided() {
        verifier.shouldNotRegisterOutboxProcessorWhenCustomBeanProvided();
    }
}