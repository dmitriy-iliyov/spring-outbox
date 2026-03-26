package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.starter.it.OracleTestContainerSingleton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OracleOutboxDlqAutoConfigurationIntegrationTests {

    private final OutboxDlqAutoConfigurationVerifier verifier =
            new OutboxDlqAutoConfigurationVerifier(
                    OracleTestContainerSingleton.INSTANCE.getJdbcUrl(),
                    "oracle.jdbc.OracleDriver",
                    OracleTestContainerSingleton.INSTANCE.getUsername(),
                    OracleTestContainerSingleton.INSTANCE.getPassword()
            );

    @Test
    @DisplayName("IT should not load DLQ config when dlq.enabled=false")
    void shouldNotLoadWhenDisabled() {
        verifier.shouldNotLoadWhenDisabled();
    }

    @Test
    @DisplayName("IT should register OutboxDlqRepository bean")
    void shouldRegisterOutboxDlqRepository() {
        verifier.shouldRegisterOutboxDlqRepository();
    }

    @Test
    @DisplayName("IT should register OutboxDlqManager bean")
    void shouldRegisterOutboxDlqManager() {
        verifier.shouldRegisterOutboxDlqManager();
    }

    @Test
    @DisplayName("IT should register OutboxDlqHandler bean with default log handler")
    void shouldRegisterDefaultOutboxDlqHandler() {
        verifier.shouldRegisterDefaultOutboxDlqHandler();
    }

    @Test
    @DisplayName("IT should register OutboxDlqTransfer bean")
    void shouldRegisterOutboxDlqTransfer() {
        verifier.shouldRegisterOutboxDlqTransfer();
    }

    @Test
    @DisplayName("IT should register outboxDlqScheduler bean")
    void shouldRegisterOutboxDlqScheduler() {
        verifier.shouldRegisterOutboxDlqScheduler();
    }

    @Test
    @DisplayName("IT should not register duplicate OutboxDlqHandler when custom bean provided")
    void shouldNotRegisterDuplicateHandlerWhenCustomBeanProvided() {
        verifier.shouldNotRegisterDuplicateHandlerWhenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should not register duplicate OutboxDlqManager when custom bean provided")
    void shouldNotRegisterDuplicateDlqManagerWhenCustomBeanProvided() {
        verifier.shouldNotRegisterDuplicateDlqManagerWhenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should create tables when auto-create is true")
    void shouldCreateTablesWhenAutoCreateTrue() {
        verifier.shouldCreateTablesWhenAutoCreateTrue();
    }
}
