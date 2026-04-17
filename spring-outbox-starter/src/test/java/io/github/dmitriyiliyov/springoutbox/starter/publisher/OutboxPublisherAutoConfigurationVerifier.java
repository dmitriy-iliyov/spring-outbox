package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.aop.OutboxPublishAspect;
import io.github.dmitriyiliyov.springoutbox.aop.RowOutboxEventListener;
import io.github.dmitriyiliyov.springoutbox.core.publisher.*;
import io.github.dmitriyiliyov.springoutbox.core.publisher.utils.UuidGenerator;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.OutboxManagerMetricsDecorator;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.OutboxMetricsRepository;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.OutboxMetricsService;
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

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

public class OutboxPublisherAutoConfigurationVerifier {

    private final String dbUrl;
    private final String dbDriver;
    private final String dbUsername;
    private final String dbPassword;

    public OutboxPublisherAutoConfigurationVerifier(String dbUrl, String dbDriver, String dbUsername, String dbPassword) {
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
                        KafkaAutoConfiguration.class
                ))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withBean(Clock.class, Clock::systemDefaultZone)
                .withPropertyValues(
                        "spring.datasource.url=" + dbUrl,
                        "spring.datasource.driver-class-name=" + dbDriver,
                        "spring.datasource.username=" + dbUsername,
                        "spring.datasource.password=" + dbPassword,
                        "outbox.tables.auto-create=false",
                        "outbox.publisher.enabled=true",
                        "outbox.publisher.sender.type=kafka",
                        "outbox.publisher.events.my-event.topic=my.topic"
                );
    }

    public void shouldLoadMinimalConfiguration_whenOnlyRequiredPropertiesSet() {
        getBaseContextRunner()
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasSingleBean(OutboxRepository.class);
                    assertThat(ctx).hasSingleBean(OutboxManager.class);
                    assertThat(ctx).hasSingleBean(OutboxProcessor.class);
                    assertThat(ctx).hasSingleBean(OutboxSender.class);
                    assertThat(ctx).hasSingleBean(OutboxSerializer.class);
                    assertThat(ctx).hasSingleBean(OutboxPublisher.class);
                    assertThat(ctx).hasSingleBean(UuidGenerator.class);
                    assertThat(ctx).hasSingleBean(OutboxPublishAspect.class);
                    assertThat(ctx).hasSingleBean(RowOutboxEventListener.class);

                    assertThat(ctx).hasBean("outboxRecoveryScheduler");
                    assertThat(ctx).hasBean("myeventOutboxPublisherScheduler");
                    assertThat(ctx).hasBean("outboxCleanUpScheduler");

                    assertThat(ctx).doesNotHaveBean(OutboxManagerMetricsDecorator.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetricsService.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetricsRepository.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetrics.class);
                });
    }

    public void shouldLoadFullConfiguration_whenAllFeaturesEnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "outbox.publisher.metrics.enabled=true",
                        "outbox.publisher.metrics.gauge.enabled=true",
                        "outbox.publisher.metrics.gauge.cache.ttls[0]=PT10S",
                        "outbox.publisher.metrics.gauge.cache.ttls[1]=PT30S",
                        "outbox.publisher.metrics.gauge.cache.ttls[2]=PT60S",
                        "outbox.publisher.clean-up.enabled=true",
                        "outbox.publisher.clean-up.interval=PT1M",
                        "outbox.publisher.clean-up.retention=PT24H",
                        "outbox.publisher.events.my-event.topic=my.topic",
                        "outbox.publisher.events.other-event.topic=other.topic"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasSingleBean(OutboxRepository.class);
                    assertThat(ctx).hasSingleBean(OutboxProcessor.class);
                    assertThat(ctx).hasSingleBean(OutboxSender.class);
                    assertThat(ctx).hasSingleBean(OutboxSerializer.class);
                    assertThat(ctx).hasSingleBean(OutboxPublisher.class);
                    assertThat(ctx).hasSingleBean(UuidGenerator.class);
                    assertThat(ctx).hasSingleBean(OutboxPublishAspect.class);
                    assertThat(ctx).hasSingleBean(RowOutboxEventListener.class);

                    assertThat(ctx).hasBean("defaultOutboxManager");
                    assertThat(ctx).hasBean("outboxManagerMetricsDecorator");
                    OutboxManager primary = ctx.getBean(OutboxManager.class);
                    assertThat(primary).isInstanceOf(OutboxManagerMetricsDecorator.class);

                    assertThat(ctx).hasSingleBean(OutboxManagerMetricsDecorator.class);
                    assertThat(ctx).hasSingleBean(OutboxMetricsService.class);
                    assertThat(ctx).hasSingleBean(OutboxMetricsRepository.class);
                    assertThat(ctx).hasSingleBean(OutboxMetrics.class);

                    assertThat(ctx).hasBean("outboxRecoveryScheduler");
                    assertThat(ctx).hasBean("outboxCleanUpScheduler");
                    assertThat(ctx).hasBean("myeventOutboxPublisherScheduler");
                    assertThat(ctx).hasBean("othereventOutboxPublisherScheduler");
                });
    }

    public void shouldLoadConfiguration_whenMetricsEnabledButGaugeDisabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "outbox.publisher.metrics.enabled=true",
                        "outbox.publisher.metrics.gauge.enabled=false"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    OutboxManager primary = ctx.getBean(OutboxManager.class);
                    assertThat(primary).isInstanceOf(OutboxManagerMetricsDecorator.class);

                    assertThat(ctx).doesNotHaveBean(OutboxMetricsService.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetricsRepository.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetrics.class);
                });
    }

    public void shouldLoadConfiguration_whenCleanUpEnabledButMetricsDisabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "outbox.publisher.clean-up.enabled=true",
                        "outbox.publisher.clean-up.interval=PT1M",
                        "outbox.publisher.clean-up.retention=PT24H"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasBean("outboxCleanUpScheduler");
                    assertThat(ctx).doesNotHaveBean(OutboxManagerMetricsDecorator.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetricsService.class);

                    OutboxManager primary = ctx.getBean(OutboxManager.class);
                    assertThat(primary).isInstanceOf(DefaultOutboxManager.class);
                });
    }

    public void shouldNotLoad_whenDisabled() {
        getBaseContextRunner()
                .withPropertyValues("outbox.publisher.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(OutboxProcessor.class);
                    assertThat(ctx).doesNotHaveBean(OutboxManager.class);
                    assertThat(ctx).doesNotHaveBean(OutboxSender.class);
                    assertThat(ctx).doesNotHaveBean(OutboxManagerMetricsDecorator.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetricsService.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetricsRepository.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetrics.class);
                });
    }

    public void shouldRegisterOutboxRepository() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxRepository.class)
        );
    }

    public void shouldRegisterOutboxManager() {
        getBaseContextRunner().run(ctx -> {
            assertThat(ctx).hasSingleBean(OutboxManager.class);
            assertThat(ctx).doesNotHaveBean(OutboxManagerMetricsDecorator.class);
            assertThat(ctx).doesNotHaveBean(OutboxMetricsService.class);
            assertThat(ctx).doesNotHaveBean(OutboxMetricsRepository.class);
            assertThat(ctx).doesNotHaveBean(OutboxMetrics.class);
        });
    }

    public void shouldRegisterOutboxProcessor() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxProcessor.class)
        );
    }

    public void shouldRegisterOutboxSerializer() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxSerializer.class)
        );
    }

    public void shouldRegisterOutboxPublisher() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxPublisher.class)
        );
    }

    public void shouldRegisterUuidGenerator() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(UuidGenerator.class)
        );
    }

    public void shouldRegisterOutboxPublishAspect() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxPublishAspect.class)
        );
    }

    public void shouldRegisterRowOutboxEventListener() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(RowOutboxEventListener.class)
        );
    }

    public void shouldRegisteredMetricsRelatedBeans_whenAllMetricsEnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "outbox.publisher.metrics.enabled=true",
                        "outbox.publisher.metrics.gauge.enabled=true"
                )
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxManagerMetricsDecorator.class);
                    assertThat(ctx).hasSingleBean(OutboxMetricsService.class);
                    assertThat(ctx).hasSingleBean(OutboxMetricsRepository.class);
                    assertThat(ctx).hasSingleBean(OutboxMetrics.class);
                });
    }

    public void shouldRegisteredMetricsRelatedBeans_whenGaugeUnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "outbox.publisher.metrics.enabled=true",
                        "outbox.publisher.metrics.gauge.enabled=false"
                )
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxManagerMetricsDecorator.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetricsService.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetricsRepository.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetrics.class);
                });
    }

    public void shouldRegisteredMetricsRelatedBeans_whenGaugeEnabledMissed() {
        getBaseContextRunner()
                .withPropertyValues(
                        "outbox.publisher.metrics.enabled=true"
                )
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxManagerMetricsDecorator.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetricsService.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetricsRepository.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetrics.class);
                });
    }

    public void shouldNotRegisterOutboxRepository_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customOutboxRepository", OutboxRepository.class, () -> org.mockito.Mockito.mock(OutboxRepository.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxRepository.class);
                    assertThat(ctx).hasBean("customOutboxRepository");
                });
    }

    public void shouldNotRegisterOutboxProcessor_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customOutboxProcessor", OutboxProcessor.class, () -> org.mockito.Mockito.mock(OutboxProcessor.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxProcessor.class);
                    assertThat(ctx).hasBean("customOutboxProcessor");
                });
    }

    public void shouldNotRegisterOutboxManager_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customOutboxManager", OutboxManager.class, () -> org.mockito.Mockito.mock(OutboxManager.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxManager.class);
                    assertThat(ctx).hasBean("customOutboxManager");
                    assertThat(ctx).doesNotHaveBean(DefaultOutboxManager.class);
                });
    }

    public void shouldNotRegisterOutboxSender_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customOutboxSender", OutboxSender.class, () -> org.mockito.Mockito.mock(OutboxSender.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxSender.class);
                    assertThat(ctx).hasBean("customOutboxSender");
                });
    }

    public void shouldNotRegisterOutboxSerializer_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customOutboxSerializer", OutboxSerializer.class, () -> org.mockito.Mockito.mock(OutboxSerializer.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxSerializer.class);
                    assertThat(ctx).hasBean("customOutboxSerializer");
                });
    }

    public void shouldNotRegisterUuidGenerator_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customUuidGenerator", UuidGenerator.class, () -> org.mockito.Mockito.mock(UuidGenerator.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(UuidGenerator.class);
                    assertThat(ctx).hasBean("customUuidGenerator");
                });
    }

    public void shouldRegisterOutboxManager_whenMetricsEnabled_hasDecoratorAsPrimary() {
        getBaseContextRunner()
                .withPropertyValues("outbox.publisher.metrics.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasBean("defaultOutboxManager");
                    assertThat(ctx).hasBean("outboxManagerMetricsDecorator");
                    OutboxManager primary = ctx.getBean(OutboxManager.class);
                    assertThat(primary).isInstanceOf(OutboxManagerMetricsDecorator.class);
                });
    }

    public void shouldRegisterPublisherScheduler_perEventType() {
        getBaseContextRunner()
                .withPropertyValues(
                        "outbox.publisher.events.my-event.topic=my.topic",
                        "outbox.publisher.events.other-event.topic=other.topic"
                )
                .run(ctx -> {
                    assertThat(ctx).hasBean("myeventOutboxPublisherScheduler");
                    assertThat(ctx).hasBean("othereventOutboxPublisherScheduler");
                });
    }

    public void shouldRegisterRecoveryScheduler() {
        getBaseContextRunner()
                .run(ctx ->
                        assertThat(ctx).hasBean("outboxRecoveryScheduler")
                );
    }

    public void shouldRegisterCleanUpScheduler_whenEnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "outbox.publisher.clean-up.enabled=true",
                        "outbox.publisher.clean-up.interval=PT1M",
                        "outbox.publisher.clean-up.retention=PT24H"
                )
                .run(ctx ->
                        assertThat(ctx).hasBean("outboxCleanUpScheduler")
                );
    }

    public void shouldNotRegisterCleanUpScheduler_whenDisabled() {
        getBaseContextRunner()
                .withPropertyValues("outbox.publisher.clean-up.enabled=false")
                .run(ctx ->
                        assertThat(ctx).doesNotHaveBean("outboxCleanUpScheduler")
                );
    }

    public void shouldRegisterPublisherScheduler_withMetricsDecoratorAsManager() {
        getBaseContextRunner()
                .withPropertyValues(
                        "outbox.publisher.metrics.enabled=true",
                        "outbox.publisher.events.my-event.topic=my.topic"
                )
                .run(ctx -> {
                    assertThat(ctx).hasBean("myeventOutboxPublisherScheduler");
                    assertThat(ctx).hasBean("outboxRecoveryScheduler");
                    OutboxManager primary = ctx.getBean(OutboxManager.class);
                    assertThat(primary).isInstanceOf(OutboxManagerMetricsDecorator.class);
                });
    }
}