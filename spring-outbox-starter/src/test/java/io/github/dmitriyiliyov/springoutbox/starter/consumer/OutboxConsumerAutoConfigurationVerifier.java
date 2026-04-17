package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.*;
import io.github.dmitriyiliyov.springoutbox.metrics.consumer.ConsumedOutboxManagerMetricsDecorator;
import io.github.dmitriyiliyov.springoutbox.metrics.consumer.OutboxIdempotentConsumerMetricsDecorator;
import io.github.dmitriyiliyov.springoutbox.starter.OutboxAutoConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
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
                        OutboxConsumerAutoConfiguration.class
                ))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withBean(CacheManager.class, () -> new ConcurrentMapCacheManager("outbox"))
                .withBean(Clock.class, Clock::systemDefaultZone)
                .withPropertyValues(
                        "spring.datasource.url=" + dbUrl,
                        "spring.datasource.driver-class-name=" + dbDriver,
                        "spring.datasource.username=" + dbUsername,
                        "spring.datasource.password=" + dbPassword,
                        "outbox.tables.auto-create=false",
                        "outbox.consumer.enabled=true",
                        "outbox.consumer.cache.cache-name=outbox"
                );
    }

    public void shouldLoadFullConfiguration_whenAllFeaturesEnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "outbox.consumer.metrics.enabled=true",
                        "outbox.consumer.cache.enabled=true",
                        "outbox.consumer.cache.cache-name=outbox",
                        "outbox.consumer.clean-up.enabled=true",
                        "outbox.consumer.clean-up.interval=PT1M",
                        "outbox.consumer.clean-up.retention=PT24H"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasSingleBean(ConsumedOutboxRepository.class);
                    assertThat(ctx).hasSingleBean(OutboxEventIdResolveManager.class);
                    assertThat(ctx).hasSingleBean(DefaultConsumedOutboxManager.class);
                    assertThat(ctx).hasSingleBean(ConsumedOutboxManagerMetricsDecorator.class);
                    assertThat(ctx).hasSingleBean(DefaultOutboxIdempotentConsumer.class);
                    assertThat(ctx).hasSingleBean(OutboxIdempotentConsumerMetricsDecorator.class);

                    assertThat(ctx).hasSingleBean(ConsumedOutboxManagerCacheDecoratorSupplier.class);
                    assertThat(ctx).hasSingleBean(ConsumedOutboxManagerMetricsDecoratorSupplier.class);

                    ConsumedOutboxManager primaryConsumedOutboxManager = ctx.getBean(ConsumedOutboxManager.class);
                    assertThat(primaryConsumedOutboxManager).isInstanceOf(ConsumedOutboxManagerMetricsDecorator.class);

                    OutboxIdempotentConsumer primaryOutboxIdempotentConsumer = ctx.getBean(OutboxIdempotentConsumer.class);
                    assertThat(primaryOutboxIdempotentConsumer).isInstanceOf(OutboxIdempotentConsumerMetricsDecorator.class);

                    assertThat(ctx).hasBean("kafkaOutboxEventIdResolver");
                    assertThat(ctx).hasBean("rabbitMqOutboxEventIdResolver");
                    assertThat(ctx).hasBean("springMessageOutboxEventIdResolver");

                    assertThat(ctx).hasBean("consumedOutboxCleanUpScheduler");
                });
    }

    public void shouldLoadMinimalConfiguration_whenOnlyRequiredPropertiesSet() {
        getBaseContextRunner()
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasSingleBean(ConsumedOutboxRepository.class);
                    assertThat(ctx).hasBean("consumedOutboxManager");
                    assertThat(ctx).hasBean("primaryConsumedOutboxManager");

                    ConsumedOutboxManager primaryConsumedOutboxManager = ctx.getBean(ConsumedOutboxManager.class);
                    assertThat(primaryConsumedOutboxManager).isInstanceOf(DefaultConsumedOutboxManager.class);

                    assertThat(ctx).hasSingleBean(OutboxIdempotentConsumer.class);
                    assertThat(ctx).hasSingleBean(OutboxEventIdResolveManager.class);

                    assertThat(ctx).doesNotHaveBean(ConsumedOutboxManagerMetricsDecoratorSupplier.class);
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
                .withPropertyValues(
                        "spring.datasource.url=" + dbUrl,
                        "spring.datasource.driver-class-name=" + dbDriver,
                        "spring.datasource.username=" + dbUsername,
                        "spring.datasource.password=" + dbPassword,
                        "outbox.tables.auto-create=false",
                        "outbox.consumer.enabled=false"
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
            assertThat(ctx).doesNotHaveBean(ConsumedOutboxManagerDecoratorSupplier.class);
            assertThat(ctx).hasBean("consumedOutboxManager");
            assertThat(ctx).hasBean("primaryConsumedOutboxManager");

            ConsumedOutboxManager primaryConsumedOutboxManager = ctx.getBean(ConsumedOutboxManager.class);
            assertThat(primaryConsumedOutboxManager).isInstanceOf(DefaultConsumedOutboxManager.class);
        });
    }

    public void shouldRegisterOutboxIdempotentConsumer() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxIdempotentConsumer.class)
        );
    }

    public void shouldRegisterOutboxEventIdResolveManager() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxEventIdResolveManager.class)
        );
    }

    public void shouldRegisterKafkaEventIdResolver() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasBean("kafkaOutboxEventIdResolver")
        );
    }

    public void shouldRegisterRabbitMqEventIdResolver() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasBean("rabbitMqOutboxEventIdResolver")
        );
    }

    public void shouldNotRegisterCleanUpScheduler_whenDisabled() {
        getBaseContextRunner()
                .withPropertyValues("outbox.consumer.clean-up.enabled=false")
                .run(ctx ->
                        assertThat(ctx).doesNotHaveBean("consumedOutboxCleanUpScheduler")
                );
    }

    public void shouldNotRegisterCleanUpScheduler_whenEnabled() {
        getBaseContextRunner()
                .withPropertyValues("outbox.consumer.clean-up.enabled=true")
                .run(ctx ->
                        assertThat(ctx).hasBean("consumedOutboxCleanUpScheduler")
                );
    }

    public void shouldNotRegisterDuplicateConsumedOutboxManager_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean(
                        "customConsumedOutboxManager",
                        ConsumedOutboxManager.class,
                        () -> org.mockito.Mockito.mock(ConsumedOutboxManager.class)
                )
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(ConsumedOutboxManagerDecoratorSupplier.class);
                    assertThat(ctx).hasBean("customConsumedOutboxManager");
                    assertThat(ctx).hasBean("primaryConsumedOutboxManager");
                });
    }

    public void shouldCreateTables_whenAutoCreateTrue() {
        getBaseContextRunner()
                .withPropertyValues("outbox.tables.auto-create=true")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    JdbcTemplate jdbcTemplate = ctx.getBean("outboxJdbcTemplate", JdbcTemplate.class);
                    jdbcTemplate.execute("SELECT 1 FROM outbox_consumed_events WHERE 1=0");
                });
    }

    public void shouldNotRegisteredMetricsRelatedBeans_whenMetricsDisabled() {
        getBaseContextRunner()
                .withPropertyValues("outbox.consumer.metrics.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(ConsumedOutboxManagerMetricsDecoratorSupplier.class);
                    assertThat(ctx).doesNotHaveBean(ConsumedOutboxManagerMetricsDecorator.class);
                });
    }

    public void shouldRegisteredMetricsRelatedBeans_whenMetricsEnabled() {
        getBaseContextRunner()
                .withPropertyValues("outbox.consumer.metrics.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ConsumedOutboxManagerMetricsDecoratorSupplier.class);
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
                    assertThat(ctx).hasSingleBean(OutboxIdempotentConsumer.class);
                    assertThat(ctx).hasBean("customOutboxIdempotentConsumer");
                });
    }

    public void shouldNotRegisterDuplicateOutboxEventIdResolveManager_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customOutboxEventIdResolveManager", OutboxEventIdResolveManager.class,
                        () -> org.mockito.Mockito.mock(OutboxEventIdResolveManager.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxEventIdResolveManager.class);
                    assertThat(ctx).hasBean("customOutboxEventIdResolveManager");
                });
    }

    public void shouldRegisterConsumedOutboxManagerDecorator_asPrimary_whenMetricsEnabled() {
        getBaseContextRunner()
                .withPropertyValues("outbox.consumer.metrics.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ConsumedOutboxManagerMetricsDecoratorSupplier.class);
                    assertThat(ctx).hasBean("consumedOutboxManager");
                    assertThat(ctx).hasBean("primaryConsumedOutboxManager");
                    ConsumedOutboxManager primary = ctx.getBean(ConsumedOutboxManager.class);
                    assertThat(primary).isInstanceOf(ConsumedOutboxManagerMetricsDecorator.class);
                });
    }

    public void shouldRegisterOutboxIdempotentConsumerMetricsDecorator_whenMetricsEnabled() {
        getBaseContextRunner()
                .withPropertyValues("outbox.consumer.metrics.enabled=true")
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(OutboxIdempotentConsumerMetricsDecorator.class)
                );
    }

    public void shouldNotRegisterOutboxIdempotentConsumerMetricsDecorator_whenMetricsDisabled() {
        getBaseContextRunner()
                .withPropertyValues("outbox.consumer.metrics.enabled=false")
                .run(ctx ->
                        assertThat(ctx).doesNotHaveBean(OutboxIdempotentConsumerMetricsDecorator.class)
                );
    }

    public void shouldRegisterSpringMessageEventIdResolver() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasBean("springMessageOutboxEventIdResolver")
        );
    }

    public void shouldRegisteredConsumedOutboxManagerCacheDecorator_asPrimary_whenCacheEnableAndMetricsDisabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "outbox.consumer.metrics.enabled=false",
                        "outbox.consumer.cache.enabled=true",
                        "outbox.consumer.cache.cache-name=outbox",
                        "outbox.consumer.clean-up.enabled=true",
                        "outbox.consumer.clean-up.interval=PT1M",
                        "outbox.consumer.clean-up.retention=PT24H"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasSingleBean(ConsumedOutboxRepository.class);
                    assertThat(ctx).hasSingleBean(OutboxEventIdResolveManager.class);
                    assertThat(ctx).hasSingleBean(DefaultConsumedOutboxManager.class);
                    assertThat(ctx).hasSingleBean(DefaultOutboxIdempotentConsumer.class);
                    assertThat(ctx).doesNotHaveBean(OutboxIdempotentConsumerMetricsDecorator.class);

                    assertThat(ctx).hasSingleBean(ConsumedOutboxManagerCacheDecoratorSupplier.class);
                    assertThat(ctx).doesNotHaveBean(ConsumedOutboxManagerMetricsDecoratorSupplier.class);

                    ConsumedOutboxManager primaryConsumedOutboxManager = ctx.getBean(ConsumedOutboxManager.class);
                    System.out.println(primaryConsumedOutboxManager.getClass().getName());
                    assertThat(primaryConsumedOutboxManager).isInstanceOf(ConsumedOutboxManagerCacheDecorator.class);

                    OutboxIdempotentConsumer primaryOutboxIdempotentConsumer = ctx.getBean(OutboxIdempotentConsumer.class);
                    assertThat(primaryOutboxIdempotentConsumer).isInstanceOf(DefaultOutboxIdempotentConsumer.class);

                    assertThat(ctx).hasBean("kafkaOutboxEventIdResolver");
                    assertThat(ctx).hasBean("rabbitMqOutboxEventIdResolver");
                    assertThat(ctx).hasBean("springMessageOutboxEventIdResolver");

                    assertThat(ctx).hasBean("consumedOutboxCleanUpScheduler");
                });
    }

    public void shouldRegisteredConsumedOutboxManagerCacheDecorator_asPrimary_whenCacheEnableAndMetricsMissed() {
        getBaseContextRunner()
                .withPropertyValues(
                        "outbox.consumer.cache.enabled=true",
                        "outbox.consumer.cache.cache-name=outbox",
                        "outbox.consumer.clean-up.enabled=true",
                        "outbox.consumer.clean-up.interval=PT1M",
                        "outbox.consumer.clean-up.retention=PT24H"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasSingleBean(ConsumedOutboxRepository.class);
                    assertThat(ctx).hasSingleBean(OutboxEventIdResolveManager.class);
                    assertThat(ctx).hasSingleBean(DefaultConsumedOutboxManager.class);
                    assertThat(ctx).doesNotHaveBean(ConsumedOutboxManagerMetricsDecorator.class);
                    assertThat(ctx).hasSingleBean(DefaultOutboxIdempotentConsumer.class);
                    assertThat(ctx).doesNotHaveBean(OutboxIdempotentConsumerMetricsDecorator.class);

                    assertThat(ctx).hasSingleBean(ConsumedOutboxManagerCacheDecoratorSupplier.class);
                    assertThat(ctx).doesNotHaveBean(ConsumedOutboxManagerMetricsDecoratorSupplier.class);

                    ConsumedOutboxManager primaryConsumedOutboxManager = ctx.getBean(ConsumedOutboxManager.class);
                    assertThat(primaryConsumedOutboxManager).isInstanceOf(ConsumedOutboxManagerCacheDecorator.class);

                    OutboxIdempotentConsumer primaryOutboxIdempotentConsumer = ctx.getBean(OutboxIdempotentConsumer.class);
                    assertThat(primaryOutboxIdempotentConsumer).isInstanceOf(DefaultOutboxIdempotentConsumer.class);

                    assertThat(ctx).hasBean("kafkaOutboxEventIdResolver");
                    assertThat(ctx).hasBean("rabbitMqOutboxEventIdResolver");
                    assertThat(ctx).hasBean("springMessageOutboxEventIdResolver");

                    assertThat(ctx).hasBean("consumedOutboxCleanUpScheduler");
                });
    }
}