package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.core.consumer.*;
import io.github.dmitriyiliyov.springoutbox.kafka.KafkaOutboxEventIdResolver;
import io.github.dmitriyiliyov.springoutbox.messaging.SpringMessageOutboxEventIdResolver;
import io.github.dmitriyiliyov.springoutbox.metrics.consumer.MetricsConsumedOutboxCacheObserver;
import io.github.dmitriyiliyov.springoutbox.metrics.consumer.OutboxIdempotentConsumerMetricsDecorator;
import io.github.dmitriyiliyov.springoutbox.rabbit.RabbitMqOutboxEventIdResolver;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ConditionalOnProperty(prefix = "outbox.consumer", name = "enabled", havingValue = "true")
public class OutboxConsumerAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OutboxConsumerAutoConfiguration.class);

    private final OutboxConsumerProperties properties;

    public OutboxConsumerAutoConfiguration(OutboxConsumerProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsumedOutboxRepository consumedOutboxRepository(
            DataSource dataSource,
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
            Clock clock
    ) {
        return ConsumedOutboxRepositoryFactory.generate(dataSource, jdbcTemplate, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsumedOutboxManager consumedOutboxManager(ConsumedOutboxRepository repository, Clock clock) {
        return new DefaultConsumedOutboxManager(repository, clock);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "outbox.consumer.metrics",
            name = "enabled",
            havingValue = "true"
    )
    public ConsumedOutboxCacheObserver metricsConsumedOutboxCacheObserver(MeterRegistry registry) {
        return new MetricsConsumedOutboxCacheObserver(registry);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "outbox.consumer.metrics",
            name = "enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    public ConsumedOutboxCacheObserver noopConsumedOutboxCacheObserver() {
        return NoopConsumedOutboxCacheObserver.INSTANCE;
    }

    @Bean
    @Order(1)
    @ConditionalOnProperty(
            prefix = "outbox.consumer.cache",
            name = "enabled",
            havingValue = "true"
    )
    public ConsumedOutboxManagerDecoratorSupplier consumedOutboxManagerCacheDecoratorFactory(
            CacheManager cacheManager,
            ConsumedOutboxCacheObserver cacheObserver
    ) {
        return new ConsumedOutboxManagerCacheDecoratorSupplier(
                cacheManager,
                properties.getCache().getCacheName(),
                cacheObserver
        );
    }

    @Bean
    @Order(2)
    @ConditionalOnProperty(
            prefix = "outbox.consumer.metrics",
            name = "enabled",
            havingValue = "true"
    )
    public ConsumedOutboxManagerDecoratorSupplier consumedOutboxManagerMetricsDecorator(MeterRegistry registry) {
        return new ConsumedOutboxManagerMetricsDecoratorSupplier(registry);
    }

    @Bean
    @Primary
    public ConsumedOutboxManager primaryConsumedOutboxManager(
            ConsumedOutboxManager consumedOutboxManager,
            List<ConsumedOutboxManagerDecoratorSupplier> suppliers
    ) {
        ConsumedOutboxManager primaryConsumedOutboxManager = consumedOutboxManager;
        for (ConsumedOutboxManagerDecoratorSupplier supplier : suppliers) {
            primaryConsumedOutboxManager = supplier.supply(primaryConsumedOutboxManager);
        }
        return primaryConsumedOutboxManager;
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxIdempotentConsumer outboxIdempotentConsumer(OutboxEventIdResolveManager idResolver,
                                                             TransactionTemplate transactionTemplate,
                                                             ConsumedOutboxManager manager) {
        return new DefaultOutboxIdempotentConsumer(idResolver, transactionTemplate, manager);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(
            prefix = "outbox.consumer.metrics",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxIdempotentConsumer outboxIdempotentConsumerMetricsDecorator(OutboxIdempotentConsumer consumer,
                                                                             MeterRegistry registry) {
        return new OutboxIdempotentConsumerMetricsDecorator(registry, consumer);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.amqp.core.Message")
    public OutboxEventIdResolver<?> rabbitMqOutboxEventIdResolver() {
        return new RabbitMqOutboxEventIdResolver();
    }

    @Bean
    @ConditionalOnClass(name = "org.apache.kafka.clients.consumer.ConsumerRecord")
    public OutboxEventIdResolver<?> kafkaOutboxEventIdResolver() {
        return new KafkaOutboxEventIdResolver();
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.messaging.Message")
    public OutboxEventIdResolver<?> springMessageOutboxEventIdResolver() {
        return new SpringMessageOutboxEventIdResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxEventIdResolveManager defaultOutboxEventIdResolveManager(List<OutboxEventIdResolver<?>> resolvers) {
        if (resolvers.isEmpty()) {
            throw new IllegalArgumentException("No common message types detected in the classpath");
        }
        log.debug("OutboxEventIdResolveManager configured with resolvers: {}", resolvers);
        return new DefaultOutboxEventIdResolveManager(resolvers);
    }

    @Bean
    @ConditionalOnProperty(prefix = "outbox.consumer.clean-up", name = "enabled", havingValue = "true")
    public OutboxScheduler consumedOutboxCleanUpScheduler(
            @Qualifier("outboxScheduledExecutorService") ScheduledExecutorService executor,
            ConsumedOutboxManager manager
    ) {
        return new ConsumedOutboxCleanUpScheduler(properties.getCleanUp(), executor, manager);
    }
}
