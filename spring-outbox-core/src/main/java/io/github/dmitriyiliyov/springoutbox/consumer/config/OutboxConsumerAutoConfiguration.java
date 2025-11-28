package io.github.dmitriyiliyov.springoutbox.consumer.config;

import io.github.dmitriyiliyov.springoutbox.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.consumer.*;
import io.github.dmitriyiliyov.springoutbox.consumer.metrics.ConsumedOutboxManagerMetricsDecorator;
import io.github.dmitriyiliyov.springoutbox.consumer.metrics.OutboxIdempotentConsumerMetricsDecorator;
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
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
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
    public ConsumedOutboxRepository consumedOutboxRepository(DataSource dataSource) {
        return ConsumedOutboxRepositoryFactory.generate(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsumedOutboxManager consumedOutboxManager(CacheManager cacheManager, ConsumedOutboxRepository repository,
                                                                MeterRegistry registry) {
        OutboxConsumerProperties.CacheProperties cacheProperties = properties.getCache();
        ConsumedOutboxManager manager = new DefaultConsumedOutboxManager(repository);
        if (cacheManager == null || !cacheProperties.isEnabled()) {
            if (cacheManager == null) {
                log.error("CacheManager is null, impossible to create bean of CacheableConsumedOutboxManager");
            }
            return new ConsumedOutboxManagerMetricsDecorator(manager, registry);
        }
        return new ConsumedOutboxManagerMetricsDecorator(
                new ConsumedOutboxManagerCacheDecorator(cacheManager, cacheProperties.getCacheName(), manager, registry),
                registry
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxIdempotentConsumer outboxIdempotentConsumer(@Qualifier("defaultOutboxEventIdResolveManager")
                                                                 OutboxEventIdResolveManager idResolver,
                                                             TransactionTemplate transactionTemplate,
                                                             ConsumedOutboxManager consumedOutboxManager,
                                                             MeterRegistry registry) {
        return new OutboxIdempotentConsumerMetricsDecorator(
                new DefaultOutboxIdempotentConsumer(idResolver, transactionTemplate, consumedOutboxManager),
                registry
        );
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
        log.info("OutboxEventIdResolveManager configured with resolvers: {}", resolvers);
        return new DefaultOutboxEventIdResolveManager(resolvers);
    }

    @Bean
    @ConditionalOnProperty(prefix = "outbox.consumer.clean-up", name = "enabled", havingValue = "true")
    public OutboxScheduler consumedOutboxCleanUpScheduler(@Qualifier("outboxScheduledExecutorService")
                                                              ScheduledExecutorService executor,
                                                          ConsumedOutboxManager consumedOutboxManager) {
        return new ConsumedOutboxCleanUpScheduler(properties.getCleanUp(), executor, consumedOutboxManager);
    }
}
