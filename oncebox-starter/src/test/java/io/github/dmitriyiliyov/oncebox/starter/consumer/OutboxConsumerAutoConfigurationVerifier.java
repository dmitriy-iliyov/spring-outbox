package io.github.dmitriyiliyov.oncebox.starter.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.consumer.cache.OutboxIdempotentConsumerCacheDecorator;
import io.github.dmitriyiliyov.oncebox.core.consumer.*;
import io.github.dmitriyiliyov.oncebox.metrics.consumer.ConsumedOutboxManagerMetricsDecorator;
import io.github.dmitriyiliyov.oncebox.metrics.consumer.MetricsConsumedOutboxCacheListener;
import io.github.dmitriyiliyov.oncebox.metrics.consumer.OutboxIdempotentConsumerMetricsDecorator;
import io.github.dmitriyiliyov.oncebox.starter.OutboxAutoConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

public class OutboxConsumerAutoConfigurationVerifier {

    private final String dbUrl;
    private final String dbDriver;
    private final String dbPassword;
    private final String dbUsername;

    public OutboxConsumerAutoConfigurationVerifier(String dbUrl, String dbDriver, String dbPassword, String dbUsername) {
        this.dbUrl = dbUrl;
        this.dbDriver = dbDriver;
        this.dbPassword = dbPassword;
        this.dbUsername = dbUsername;
    }

    private ApplicationContextRunner getBaseContextRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        DataSourceTransactionManagerAutoConfiguration.class,
                        TransactionAutoConfiguration.class,
                        CacheAutoConfiguration.class,
                        OutboxAutoConfiguration.class,
                        OutboxConsumerAutoConfiguration.class,
                        KafkaAutoConfiguration.class,
                        RabbitAutoConfiguration.class
                ))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(CacheManager.class, () -> new ConcurrentMapCacheManager("oncebox"))
                .withBean(Clock.class, Clock::systemDefaultZone)
                .withPropertyValues(
                        "spring.datasource.url=" + dbUrl,
                        "spring.datasource.driver-class-name=" + dbDriver,
                        "spring.datasource.username=" + dbUsername,
                        "spring.datasource.password=" + dbPassword,
                        "oncebox.tables.auto-create=false",
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.source.type=kafka",
                        "oncebox.consumer.mappings.test-event=io.github.dmitriyiliyov.oncebox.starter.consumer.TestEvent",
                        "oncebox.consumer.cache.cache-name=oncebox"
                );
    }

    public void shouldLoadFullConfiguration_whenAllFeaturesEnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.consumer.metrics.enabled=true",
                        "oncebox.consumer.cache.enabled=true",
                        "oncebox.consumer.cache.cache-name=oncebox",
                        "oncebox.consumer.clean-up.enabled=true",
                        "oncebox.consumer.clean-up.interval=PT1M",
                        "oncebox.consumer.clean-up.retention=PT24H"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasSingleBean(ConsumedOutboxRepository.class);
                    assertThat(ctx).hasSingleBean(DefaultConsumedOutboxManager.class);
                    assertThat(ctx).hasSingleBean(ConsumedOutboxManagerMetricsDecorator.class);
                    assertThat(ctx).hasSingleBean(DefaultOutboxIdempotentConsumer.class);
                    assertThat(ctx).hasSingleBean(MetricsConsumedOutboxCacheListener.class);

                    assertThat(ctx).hasBean("outboxKafkaRecordMessageConverter");
                    assertThat(ctx).hasBean("outboxKafkaListenerContainerFactory");
                    assertThat(ctx).doesNotHaveBean("outboxRabbitMessageConverter");
                    assertThat(ctx).doesNotHaveBean("outboxRabbitListenerContainerFactory");

                    assertThat(ctx).hasSingleBean(OutboxIdempotentConsumerCacheDecoratorSupplier.class);
                    assertThat(ctx).hasSingleBean(OutboxIdempotentConsumerMetricsDecoratorSupplier.class);

                    ConsumedOutboxManager primaryConsumedOutboxManager = ctx.getBean(ConsumedOutboxManager.class);
                    assertThat(primaryConsumedOutboxManager).isInstanceOf(ConsumedOutboxManagerMetricsDecorator.class);

                    OutboxIdempotentConsumer primaryOutboxIdempotentConsumer = ctx.getBean(OutboxIdempotentConsumer.class);
                    assertThat(primaryOutboxIdempotentConsumer).isInstanceOf(OutboxIdempotentConsumerMetricsDecorator.class);

                    assertThat(ctx).hasBean("consumedOutboxCleanUpScheduler");
                });
    }

    public void shouldLoadFullConfiguration_whenAllFeaturesWithRabbitEnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.consumer.source.type=rabbit",
                        "oncebox.consumer.metrics.enabled=true",
                        "oncebox.consumer.cache.enabled=true",
                        "oncebox.consumer.cache.cache-name=oncebox",
                        "oncebox.consumer.clean-up.enabled=true",
                        "oncebox.consumer.clean-up.interval=PT1M",
                        "oncebox.consumer.clean-up.retention=PT24H"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasSingleBean(ConsumedOutboxRepository.class);
                    assertThat(ctx).hasSingleBean(DefaultConsumedOutboxManager.class);
                    assertThat(ctx).hasSingleBean(ConsumedOutboxManagerMetricsDecorator.class);
                    assertThat(ctx).hasSingleBean(DefaultOutboxIdempotentConsumer.class);
                    assertThat(ctx).hasSingleBean(MetricsConsumedOutboxCacheListener.class);

                    assertThat(ctx).hasBean("outboxRabbitMessageConverter");
                    assertThat(ctx).hasBean("outboxRabbitListenerContainerFactory");
                    assertThat(ctx).doesNotHaveBean("outboxKafkaRecordMessageConverter");
                    assertThat(ctx).doesNotHaveBean("outboxKafkaListenerContainerFactory");

                    assertThat(ctx).hasSingleBean(OutboxIdempotentConsumerCacheDecoratorSupplier.class);
                    assertThat(ctx).hasSingleBean(OutboxIdempotentConsumerMetricsDecoratorSupplier.class);

                    ConsumedOutboxManager primaryConsumedOutboxManager = ctx.getBean(ConsumedOutboxManager.class);
                    assertThat(primaryConsumedOutboxManager).isInstanceOf(ConsumedOutboxManagerMetricsDecorator.class);

                    OutboxIdempotentConsumer primaryOutboxIdempotentConsumer = ctx.getBean(OutboxIdempotentConsumer.class);
                    assertThat(primaryOutboxIdempotentConsumer).isInstanceOf(OutboxIdempotentConsumerMetricsDecorator.class);

                    assertThat(ctx).hasBean("consumedOutboxCleanUpScheduler");
                });
    }

    public void shouldLoadMinimalConfiguration_whenOnlyRequiredPropertiesSet() {
        getBaseContextRunner()
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasSingleBean(ConsumedOutboxRepository.class);
                    assertThat(ctx).hasBean("consumedOutboxManager");
                    assertThat(ctx).hasBean("primaryOutboxIdempotentConsumer");

                    ConsumedOutboxManager primaryConsumedOutboxManager = ctx.getBean(ConsumedOutboxManager.class);
                    assertThat(primaryConsumedOutboxManager).isInstanceOf(DefaultConsumedOutboxManager.class);

                    assertThat(ctx).doesNotHaveBean(OutboxIdempotentConsumerMetricsDecoratorSupplier.class);
                    assertThat(ctx).doesNotHaveBean(ConsumedOutboxManagerMetricsDecorator.class);
                    assertThat(ctx).doesNotHaveBean(OutboxIdempotentConsumerMetricsDecorator.class);
                    assertThat(ctx).doesNotHaveBean("consumedOutboxCleanUpScheduler");
                });
    }

    public void shouldNotLoad_whenDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        DataSourceTransactionManagerAutoConfiguration.class,
                        TransactionAutoConfiguration.class,
                        CacheAutoConfiguration.class,
                        OutboxAutoConfiguration.class,
                        OutboxConsumerAutoConfiguration.class
                ))
                .withBean(Clock.class, Clock::systemDefaultZone)
                .withPropertyValues(
                        "spring.datasource.url=" + dbUrl,
                        "spring.datasource.driver-class-name=" + dbDriver,
                        "spring.datasource.username=" + dbUsername,
                        "spring.datasource.password=" + dbPassword,
                        "oncebox.tables.auto-create=false",
                        "oncebox.consumer.enabled=false"
                )
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(DefaultOutboxIdempotentConsumer.class);
                    assertThat(ctx).doesNotHaveBean(DefaultConsumedOutboxManager.class);
                    assertThat(ctx).doesNotHaveBean(ConsumedOutboxRepository.class);
                });
    }

    public void shouldRegisterConsumedOutboxRepository() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(ConsumedOutboxRepository.class)
        );
    }

    public void shouldRegisterConsumedOutboxManager() {
        getBaseContextRunner().run(ctx -> {
            assertThat(ctx).doesNotHaveBean(OutboxIdempotentConsumerDecoratorSupplier.class);
            assertThat(ctx).hasBean("consumedOutboxManager");
            assertThat(ctx).hasBean("primaryOutboxIdempotentConsumer");

            ConsumedOutboxManager primaryConsumedOutboxManager = ctx.getBean(ConsumedOutboxManager.class);
            assertThat(primaryConsumedOutboxManager).isInstanceOf(DefaultConsumedOutboxManager.class);
        });
    }

    public void shouldNotRegisterCleanUpScheduler_whenDisabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.consumer.clean-up.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean("consumedOutboxCleanUpScheduler");
                    assertThat(ctx).doesNotHaveBean("consumedOutboxCleanUpJobCreateCommand");
                });
    }

    public void shouldNotRegisterCleanUpScheduler_whenEnabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.consumer.clean-up.enabled=true")
                .run(ctx -> {
                            assertThat(ctx).hasBean("consumedOutboxCleanUpScheduler");
                            assertThat(ctx).hasBean("consumedOutboxCleanUpJobCreateCommand");
                });
    }

    public void shouldNotRegisterDuplicateConsumedOutboxManager_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean(
                        "consumedOutboxManager",
                        ConsumedOutboxManager.class,
                        () -> org.mockito.Mockito.mock(ConsumedOutboxManager.class)
                )
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(OutboxIdempotentConsumerDecoratorSupplier.class);
                    assertThat(ctx).hasBean("consumedOutboxManager");
                    assertThat(ctx).hasBean("primaryOutboxIdempotentConsumer");
                });
    }

    public void shouldCreateTables_whenAutoCreateTrue() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=true")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    JdbcTemplate jdbcTemplate = ctx.getBean("outboxJdbcTemplate", JdbcTemplate.class);
                    jdbcTemplate.execute("SELECT 1 FROM outbox_consumed_events WHERE 1=0");
                    jdbcTemplate.execute("SELECT 1 FROM outbox_jobs WHERE 1=0");
                });
    }

    public void shouldNotRegisteredMetricsRelatedBeans_whenMetricsDisabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.consumer.metrics.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(OutboxIdempotentConsumerMetricsDecoratorSupplier.class);
                    assertThat(ctx).doesNotHaveBean(ConsumedOutboxManagerMetricsDecorator.class);
                });
    }

    public void shouldRegisteredMetricsRelatedBeans_whenMetricsEnabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.consumer.metrics.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ConsumedOutboxManagerMetricsDecorator.class);
                });
    }

    public void shouldNotRegisterDuplicateConsumedOutboxRepository_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customConsumedOutboxRepository", ConsumedOutboxRepository.class,
                        () -> org.mockito.Mockito.mock(ConsumedOutboxRepository.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ConsumedOutboxRepository.class);
                    assertThat(ctx).hasBean("customConsumedOutboxRepository");
                });
    }

    public void shouldNotRegisterDuplicateOutboxIdempotentConsumer_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customOutboxIdempotentConsumer", OutboxIdempotentConsumer.class,
                        () -> org.mockito.Mockito.mock(OutboxIdempotentConsumer.class))
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean("outboxIdempotentConsumer");
                    assertThat(ctx).hasBean("customOutboxIdempotentConsumer");
                    assertThat(ctx).hasBean("primaryOutboxIdempotentConsumer");
                });
    }

    public void shouldRegisterConsumedOutboxManagerDecorator_asPrimary_whenMetricsEnabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.consumer.metrics.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasBean("consumedOutboxManager");
                    assertThat(ctx).hasBean("primaryOutboxIdempotentConsumer");
                    ConsumedOutboxManager primary = ctx.getBean(ConsumedOutboxManager.class);
                    assertThat(primary).isInstanceOf(ConsumedOutboxManagerMetricsDecorator.class);
                });
    }

    public void shouldRegisterOutboxIdempotentConsumerMetricsDecorator_whenMetricsEnabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.consumer.metrics.enabled=true")
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(OutboxIdempotentConsumerMetricsDecorator.class)
                );
    }

    public void shouldNotRegisterOutboxIdempotentConsumerMetricsDecorator_whenMetricsDisabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.consumer.metrics.enabled=false")
                .run(ctx ->
                        assertThat(ctx).doesNotHaveBean(OutboxIdempotentConsumerMetricsDecorator.class)
                );
    }

    public void shouldRegisteredConsumedOutboxManagerCacheDecorator_asPrimary_whenCacheEnableAndMetricsDisabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.consumer.metrics.enabled=false",
                        "oncebox.consumer.cache.enabled=true",
                        "oncebox.consumer.cache.cache-name=oncebox",
                        "oncebox.consumer.clean-up.enabled=true",
                        "oncebox.consumer.clean-up.interval=PT1M",
                        "oncebox.consumer.clean-up.retention=PT24H"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasSingleBean(ConsumedOutboxRepository.class);
                    assertThat(ctx).hasSingleBean(DefaultConsumedOutboxManager.class);
                    assertThat(ctx).hasSingleBean(DefaultOutboxIdempotentConsumer.class);
                    assertThat(ctx).doesNotHaveBean(OutboxIdempotentConsumerMetricsDecorator.class);

                    assertThat(ctx).hasSingleBean(OutboxIdempotentConsumerCacheDecoratorSupplier.class);
                    assertThat(ctx).doesNotHaveBean(OutboxIdempotentConsumerMetricsDecoratorSupplier.class);

                    ConsumedOutboxManager primaryConsumedOutboxManager = ctx.getBean(ConsumedOutboxManager.class);
                    assertThat(primaryConsumedOutboxManager).isInstanceOf(DefaultConsumedOutboxManager.class);

                    OutboxIdempotentConsumer primaryOutboxIdempotentConsumer = ctx.getBean(OutboxIdempotentConsumer.class);
                    assertThat(primaryOutboxIdempotentConsumer).isInstanceOf(OutboxIdempotentConsumerCacheDecorator.class);

                    assertThat(ctx).hasBean("consumedOutboxCleanUpScheduler");
                    assertThat(ctx).hasBean("consumedOutboxCleanUpJobCreateCommand");
                });
    }

    public void shouldRegisteredConsumedOutboxManagerCacheDecorator_asPrimary_whenCacheEnableAndMetricsMissed() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.consumer.cache.enabled=true",
                        "oncebox.consumer.cache.cache-name=oncebox",
                        "oncebox.consumer.clean-up.enabled=true"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasSingleBean(ConsumedOutboxRepository.class);
                    assertThat(ctx).hasSingleBean(DefaultConsumedOutboxManager.class);
                    assertThat(ctx).doesNotHaveBean(ConsumedOutboxManagerMetricsDecorator.class);
                    assertThat(ctx).hasSingleBean(DefaultOutboxIdempotentConsumer.class);
                    assertThat(ctx).doesNotHaveBean(OutboxIdempotentConsumerMetricsDecorator.class);

                    assertThat(ctx).hasSingleBean(OutboxIdempotentConsumerCacheDecoratorSupplier.class);
                    assertThat(ctx).doesNotHaveBean(OutboxIdempotentConsumerMetricsDecoratorSupplier.class);

                    ConsumedOutboxManager primaryConsumedOutboxManager = ctx.getBean(ConsumedOutboxManager.class);
                    assertThat(primaryConsumedOutboxManager).isInstanceOf(DefaultConsumedOutboxManager.class);

                    OutboxIdempotentConsumer primaryOutboxIdempotentConsumer = ctx.getBean(OutboxIdempotentConsumer.class);
                    assertThat(primaryOutboxIdempotentConsumer).isInstanceOf(OutboxIdempotentConsumerCacheDecorator.class);

                    assertThat(ctx).hasBean("consumedOutboxCleanUpScheduler");
                    assertThat(ctx).hasBean("consumedOutboxCleanUpJobCreateCommand");
                });
    }
}