package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqHandler;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqManager;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqTransfer;
import io.github.dmitriyiliyov.springoutbox.starter.OutboxAutoConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class OutboxDlqAutoConfigurationVerifier {

    private final String dbUrl;
    private final String dbDriver;
    private final String dbUsername;
    private final String dbPassword;

    public OutboxDlqAutoConfigurationVerifier(String dbUrl, String dbDriver, String dbUsername, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbDriver = dbDriver;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    private ApplicationContextRunner getBaseContextRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        DataSourceTransactionManagerAutoConfiguration.class,
                        TransactionAutoConfiguration.class,
                        JacksonAutoConfiguration.class,
                        OutboxAutoConfiguration.class,
                        OutboxPublisherAutoConfiguration.class,
                        OutboxDlqAutoConfiguration.class,
                        KafkaAutoConfiguration.class
                ))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues(
                        "spring.datasource.url=" + dbUrl,
                        "spring.datasource.driver-class-name=" + dbDriver,
                        "spring.datasource.username=" + dbUsername,
                        "spring.datasource.password=" + dbPassword,
                        "outbox.tables.auto-create=true",
                        "outbox.publisher.enabled=true",
                        "outbox.publisher.sender.type=kafka",
                        "outbox.publisher.events.my-event.topic=my.topic",
                        "outbox.publisher.dlq.enabled=true"
                );
    }

    public void shouldNotLoadWhenDisabled() {
        getBaseContextRunner()
                .withPropertyValues("outbox.publisher.dlq.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(OutboxDlqManager.class);
                    assertThat(ctx).doesNotHaveBean(OutboxDlqRepository.class);
                    assertThat(ctx).doesNotHaveBean(OutboxDlqTransfer.class);
                });
    }

    public void shouldRegisterOutboxDlqRepository() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxDlqRepository.class)
        );
    }

    public void shouldRegisterOutboxDlqManager() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxDlqManager.class)
        );
    }

    public void shouldRegisterDefaultOutboxDlqHandler() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxDlqHandler.class)
        );
    }

    public void shouldRegisterOutboxDlqTransfer() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxDlqTransfer.class)
        );
    }

    public void shouldRegisterOutboxDlqScheduler() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasBean("outboxDlqScheduler")
        );
    }

    public void shouldNotRegisterDuplicateHandlerWhenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customOutboxDlqHandler", OutboxDlqHandler.class, () -> event -> {})
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxDlqHandler.class);
                    assertThat(ctx).hasBean("customOutboxDlqHandler");
                });
    }

    public void shouldNotRegisterDuplicateDlqManagerWhenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customOutboxDlqManager", OutboxDlqManager.class, () -> org.mockito.Mockito.mock(OutboxDlqManager.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxDlqManager.class);
                    assertThat(ctx).hasBean("customOutboxDlqManager");
                });
    }

    public void shouldCreateTablesWhenAutoCreateTrue() {
        getBaseContextRunner()
                .withPropertyValues("outbox.tables.auto-create=true")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    JdbcTemplate jdbcTemplate = ctx.getBean("outboxTransactionAwareJdbcTemplate", JdbcTemplate.class);
                    jdbcTemplate.execute("SELECT 1 FROM outbox_dlq_events WHERE 1=0");
                });
    }
}