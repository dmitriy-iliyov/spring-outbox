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
    void shouldNotLoad_whenDisabled() {
        verifier.shouldNotLoad_whenDisabled();
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
    @DisplayName("IT should registered metrics related beans when all metrics enabled")
    void shouldRegisteredMetricsRelatedBeans_whenAllMetricsEnabled() {
        verifier.shouldRegisteredMetricsRelatedBeans_whenAllMetricsEnabled();
    }

    @Test
    @DisplayName("IT should registered only OutboxDlqManagerMetricsDecorator when gauge unabled")
    void shouldRegisteredMetricsRelatedBeans_whenGaugeUnabled() {
        verifier.shouldRegisteredMetricsRelatedBeans_whenGaugeUnabled();
    }

    @Test
    @DisplayName("IT should registered only OutboxDlqManagerMetricsDecorator when gauge is missed")
    void shouldRegisteredMetricsRelatedBeans_whenGaugeEnabledMissed() {
        verifier.shouldRegisteredMetricsRelatedBeans_whenGaugeEnabledMissed();
    }


    @Test
    @DisplayName("IT should not register duplicate OutboxDlqHandler when custom bean provided")
    void shouldNotRegisterDuplicateHandler_whenCustomBeanProvided() {
        verifier.shouldNotRegisterDuplicateHandler_whenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should not register duplicate OutboxDlqManager when custom bean provided")
    void shouldNotRegisterDuplicateDlqManager_whenCustomBeanProvided() {
        verifier.shouldNotRegisterDuplicateDlqManager_whenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should create tables when auto-create is true")
    void shouldCreateTablesWhenAutoCreateTrue() {
        verifier.shouldCreateTables_whenAutoCreateTrue();
    }

    @Test
    @DisplayName("IT should not register duplicate OutboxDlqTransfer when custom bean provided")
    void shouldNotRegisterDuplicateDlqTransfer_whenCustomBeanProvided() {
        verifier.shouldNotRegisterDuplicateDlqTransfer_whenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should not register duplicate OutboxDlqRepository when custom bean provided")
    void shouldNotRegisterDuplicateDlqRepository_whenCustomBeanProvided() {
        verifier.shouldNotRegisterDuplicateDlqRepository_whenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should register OutboxDlqManagerMetricsDecorator as primary when metrics enabled")
    void shouldRegisterDlqManagerDecorator_asPrimary_whenMetricsEnabled() {
        verifier.shouldRegisterDlqManagerDecorator_asPrimary_whenMetricsEnabled();
    }

    @Test
    @DisplayName("IT should register OutboxDlqTransferMetricsDecorator as primary when metrics enabled")
    void shouldRegisterDlqTransferDecorator_asPrimary_whenMetricsEnabled() {
        verifier.shouldRegisterDlqTransferDecorator_asPrimary_whenMetricsEnabled();
    }

    @Test
    @DisplayName("IT should register web layer beans when web layer is present")
    void shouldRegisterWebBeans_whenWebLayerPresent() {
        verifier.shouldRegisterWebBeans_whenWebLayerPresent();
    }
}
