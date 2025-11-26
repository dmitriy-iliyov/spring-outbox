package io.github.dmitriyiliyov.springoutbox.consumer.config;

import io.github.dmitriyiliyov.springoutbox.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.consumer.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
    public ConsumedOutboxManager cacheableConsumedOutboxManager(CacheManager cacheManager, ConsumedOutboxRepository repository) {
        OutboxConsumerProperties.CacheProperties cacheProperties = properties.getCache();
        if (cacheManager == null || !cacheProperties.isEnabled()) {
            return new DefaultConsumedOutboxManager(repository);
        }
        return new CacheableConsumedOutboxManager(cacheManager, cacheProperties.getCacheName(), repository);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsumedOutboxManager consumedOutboxManager(ConsumedOutboxRepository repository) {
        if (properties.getCache().isEnabled()) {
            log.error("CacheManager is null, impossible to create bean of CacheableConsumedOutboxManager");
        }
        return new DefaultConsumedOutboxManager(repository);
    }

    @Bean
    public OutboxIdempotentConsumer<Object> outboxIdempotentConsumer(@Qualifier("defaultOutboxEventIdResolvingManager")
                                                                         OutboxEventIdResolvingManager<Object> idResolver,
                                                                     TransactionTemplate transactionTemplate,
                                                                     ConsumedOutboxManager consumedOutboxManager) {
        return new DefaultOutboxIdempotentConsumer<>(idResolver, transactionTemplate, consumedOutboxManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public List<OutboxEventIdResolver<?>> outboxEventIdResolvers() {
        return List.of(
                new KafkaOutboxEventIdResolver<>(),
                new RabbitMqOutboxEventIdResolver(),
                new MessageOutboxEventIdResolver<>()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxEventIdResolvingManager<Object> defaultOutboxEventIdResolvingManager(List<OutboxEventIdResolver<?>> resolvers) {
        return new DefaultOutboxEventIdResolvingManager<>(resolvers);
    }

    @Bean
    @ConditionalOnProperty(prefix = "outbox.consumer.clean-up", name = "enabled", havingValue = "true")
    public OutboxScheduler consumedOutboxCleanUpScheduler(@Qualifier("outboxScheduledExecutorService")
                                                              ScheduledExecutorService executor,
                                                          ConsumedOutboxManager consumedOutboxManager) {
        return new ConsumedOutboxCleanUpScheduler(properties.getCleanUp(), executor, consumedOutboxManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxMetrics consumedOutboxMetrics(ConsumedOutboxManager manager, MeterRegistry meterRegistry) {
        return new ConsumerOutboxMetrics(manager, meterRegistry);
    }
}
