package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.springoutbox.starter.consumer.OutboxConsumerProperties;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxPublisherProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class OutboxAutoConfigurationVerifier {

    private final String dbUrl;
    private final String dbDriver;
    private final String dbUsername;
    private final String dbPassword;

    public OutboxAutoConfigurationVerifier(String dbUrl, String dbDriver, String dbUsername, String dbPassword) {
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
                        OutboxAutoConfiguration.class
                ))
                .withBean(Clock.class, Clock::systemDefaultZone)
                .withPropertyValues(
                        "spring.datasource.url=" + dbUrl,
                        "spring.datasource.driver-class-name=" + dbDriver,
                        "spring.datasource.username=" + dbUsername,
                        "spring.datasource.password=" + dbPassword
                );
    }

    public void shouldRegisterOutboxPublisherPropertiesByDefault() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxPublisherProperties.class)
        );
    }

    public void shouldRegisterOutboxPublisherPropertiesWhenEnabled() {
        getBaseContextRunner()
                .withPropertyValues("outbox.publisher.enabled=true",
                        "outbox.publisher.sender.type=kafka",
                        "outbox.publisher.events.my-event.topic=my.topic")
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(OutboxPublisherProperties.class)
                );
    }

    public void shouldNotRegisterOutboxPublisherPropertiesWhenDisabled() {
        getBaseContextRunner()
                .withPropertyValues("outbox.publisher.enabled=false")
                .run(ctx ->
                        assertThat(ctx).doesNotHaveBean(OutboxPublisherProperties.class)
                );
    }

    public void shouldNotRegisterOutboxConsumerPropertiesByDefault() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).doesNotHaveBean(OutboxConsumerProperties.class)
        );
    }

    public void shouldRegisterOutboxConsumerPropertiesWhenEnabled() {
        getBaseContextRunner()
                .withPropertyValues("outbox.consumer.enabled=true", "outbox.consumer.source.type=kafka")
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(OutboxConsumerProperties.class)
                );
    }

    public void shouldNotRegisterOutboxConsumerPropertiesWhenDisabled() {
        getBaseContextRunner()
                .withPropertyValues("outbox.consumer.enabled=false")
                .run(ctx ->
                        assertThat(ctx).doesNotHaveBean(OutboxConsumerProperties.class)
                );
    }

    public void shouldRegisterJdbcTemplate() {
        getBaseContextRunner()
                .withPropertyValues("outbox.tables.auto-create=false")
                .run(ctx ->
                        assertThat(ctx).hasBean("outboxJdbcTemplate")
                );
    }

    public void shouldNotRegisterJdbcTemplateWhenCustomBeanProvided() {
        getBaseContextRunner()
                .withPropertyValues("outbox.tables.auto-create=false")
                .withBean("outboxJdbcTemplate", JdbcTemplate.class, () -> mock(JdbcTemplate.class))
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(JdbcTemplate.class)
                );
    }

    public void shouldRegisterDataSourceInitializerByDefault() {
        getBaseContextRunner()
                .run(ctx ->
                        assertThat(ctx).hasBean("outboxDataSourceInitializer")
                );
    }

    public void shouldRegisterDataSourceInitializerWhenAutoCreateTrue() {
        getBaseContextRunner()
                .withPropertyValues("outbox.tables.auto-create=true")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasBean("outboxDataSourceInitializer");
                    JdbcTemplate jdbcTemplate = ctx.getBean("outboxJdbcTemplate", JdbcTemplate.class);
                    jdbcTemplate.execute("SELECT 1 FROM outbox_events WHERE 1=0");
                    jdbcTemplate.execute("SELECT 1 FROM outbox_jobs WHERE 1=0");
                });
    }

    public void shouldNotRegisterDataSourceInitializerWhenAutoCreateFalse() {
        getBaseContextRunner()
                .withPropertyValues("outbox.tables.auto-create=false")
                .run(ctx ->
                        assertThat(ctx).doesNotHaveBean("outboxDataSourceInitializer")
                );
    }

    public void shouldRegisterDistributedLockRepositoryWhenCleanUpEnabled() {
        getBaseContextRunner()
                .withPropertyValues("outbox.tables.auto-create=false",
                        "outbox.publisher.clean-up.enabled=true",
                        "outbox.publisher.sender.type=kafka",
                        "outbox.publisher.events.my-event.topic=my.topic"
                )
                .withBean(OutboxRepositoryFactory.class, () -> mock(OutboxRepositoryFactory.class))
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(DistributedLockRepository.class)
                );
    }

    public void shouldNotRegisterDistributedLockRepositoryWhenCleanUpDisabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "outbox.tables.auto-create=false",
                        "outbox.publisher.clean-up.enabled=false",
                        "outbox.publisher.sender.type=kafka",
                        "outbox.publisher.events.my-event.topic=my.topic"
                )
                .run(ctx ->
                        assertThat(ctx).doesNotHaveBean(DistributedLockRepository.class)
                );
    }

    public void shouldNotRegisterDistributedLockRepositoryWhenCustomBeanProvided() {
        getBaseContextRunner()
                .withPropertyValues(
                        "outbox.tables.auto-create=false",
                        "outbox.publisher.clean-up.enabled=true",
                        "outbox.publisher.sender.type=kafka",
                        "outbox.publisher.events.my-event.topic=my.topic")
                .withBean(OutboxRepositoryFactory.class, () -> mock(OutboxRepositoryFactory.class))
                .withBean(DistributedLockRepository.class, () -> mock(DistributedLockRepository.class))
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(DistributedLockRepository.class)
                );
    }

    public void shouldRegisterNoopScheduleStrategyListenerSupplierByDefault() {
        getBaseContextRunner()
                .withPropertyValues("outbox.tables.auto-create=false")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxScheduleStrategyListenerSupplier.class);
                    assertThat(ctx.getBean(OutboxScheduleStrategyListenerSupplier.class))
                            .isInstanceOf(NoopOutboxScheduleStrategyListenerSupplier.class);
                });
    }

    public void shouldRegisterMetricsScheduleStrategyListenerSupplierWhenMetricsEnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "outbox.tables.auto-create=false",
                        "outbox.publisher.metrics.enabled=true",
                        "outbox.publisher.sender.type=kafka",
                        "outbox.publisher.events.my-event.topic=my.topic"
                )
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .run(ctx -> {
                    assertThat(ctx).hasBean("metricsOutboxScheduleStrategyListenerSupplier");
                    OutboxScheduleStrategyListenerSupplier primary = ctx.getBean(OutboxScheduleStrategyListenerSupplier.class);
                    assertThat(primary).isInstanceOf(MetricsOutboxScheduleStrategyListenerSupplier.class);
                });
    }

    public void shouldNotRegisterScheduleStrategyListenerSupplierWhenCustomBeanProvided() {
        getBaseContextRunner()
                .withPropertyValues("outbox.tables.auto-create=false")
                .withBean("customScheduleStrategyListenerSupplier", OutboxScheduleStrategyListenerSupplier.class, () -> mock(OutboxScheduleStrategyListenerSupplier.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxScheduleStrategyListenerSupplier.class);
                    assertThat(ctx).hasBean("customScheduleStrategyListenerSupplier");
                });
    }

    public void shouldRegisterNoopContinuableTaskDecoratorSupplierByDefault() {
        getBaseContextRunner()
                .withPropertyValues("outbox.tables.auto-create=false")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ContinuableTaskDecoratorSupplier.class);
                    assertThat(ctx.getBean(ContinuableTaskDecoratorSupplier.class))
                            .isInstanceOf(NoopContinuableTaskDecoratorSupplier.class);
                });
    }

    public void shouldRegisterMetricsContinuableTaskDecoratorSupplierWhenMetricsEnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "outbox.tables.auto-create=false",
                        "outbox.publisher.metrics.enabled=true",
                        "outbox.publisher.sender.type=kafka",
                        "outbox.publisher.events.my-event.topic=my.topic"
                )
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues(
                        )
                .run(ctx -> {
                    assertThat(ctx).hasBean("metricsContinuableTaskDecoratorSupplier");
                    ContinuableTaskDecoratorSupplier primary = ctx.getBean(ContinuableTaskDecoratorSupplier.class);
                    assertThat(primary).isInstanceOf(ContinuableTaskTimeMeasureDecoratorSupplier.class);
                });
    }

    public void shouldNotRegisterContinuableTaskDecoratorSupplierWhenCustomBeanProvided() {
        getBaseContextRunner()
                .withPropertyValues("outbox.tables.auto-create=false")
                .withBean("customContinuableTaskDecoratorSupplier", ContinuableTaskDecoratorSupplier.class, () -> mock(ContinuableTaskDecoratorSupplier.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ContinuableTaskDecoratorSupplier.class);
                    assertThat(ctx).hasBean("customContinuableTaskDecoratorSupplier");
                });
    }

    public void shouldRegisterScheduledExecutorServiceByDefault() {
        getBaseContextRunner()
                .withPropertyValues("outbox.tables.auto-create=false")
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(ScheduledExecutorService.class)
                );
    }

    public void shouldRegisterScheduledExecutorServiceWithCustomPoolSize() {
        getBaseContextRunner()
                .withPropertyValues("outbox.tables.auto-create=false", "outbox.thread-pool-size=5")
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(ScheduledExecutorService.class)
                );
    }

    public void shouldRegisterApplicationRunnerForJobs() {
        getBaseContextRunner()
                .withPropertyValues("outbox.tables.auto-create=false")
                .run(ctx ->
                        assertThat(ctx).hasBean("outboxJobsInitializer")
                );
    }

    public void shouldRegisterOutboxInitializerWhenMissing() {
        getBaseContextRunner()
                .withPropertyValues("outbox.tables.auto-create=false")
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(SchedulersPostApplicationReadyOutboxInitializer.class)
                );
    }

    public void shouldFailWhenDataSourceNotAvailable() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class))
                .run(ctx ->
                        assertThat(ctx).hasFailed()
                );
    }
}