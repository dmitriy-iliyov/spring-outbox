package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.consumer.cache.ConsumedOutboxCache;
import io.github.dmitriyiliyov.springoutbox.consumer.cache.ConsumedOutboxCacheListener;
import io.github.dmitriyiliyov.springoutbox.consumer.cache.DefaultConsumedOutboxCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@ConditionalOnProperty(
        prefix = "outbox.consumer",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnClass(ConsumedOutboxCache.class)
@ConditionalOnProperty(
        prefix = "outbox.consumer.cache",
        name = "enabled",
        havingValue = "true"
)
public class OutboxConsumerCacheAutoConfiguration {

    private final OutboxConsumerProperties consumerProperties;

    public OutboxConsumerCacheAutoConfiguration(OutboxConsumerProperties consumerProperties) {
        this.consumerProperties = consumerProperties;
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "outbox.consumer.metrics",
            name = "enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    public ConsumedOutboxCacheListener noopConsumedOutboxCacheListener() {
        return ConsumedOutboxCacheListener.NOOP;
    }

    @Bean
    @Order(1)
    public OutboxIdempotentConsumerDecoratorSupplier outboxIdempotentConsumerCacheDecoratorSupplier(
            ConsumedOutboxCacheListener cacheListener,
            CacheManager cacheManager
    ) {
        return new OutboxIdempotentConsumerCacheDecoratorSupplier(
                new DefaultConsumedOutboxCache(
                        cacheManager,
                        consumerProperties.getCache().getCacheName(),
                        cacheListener
                )
        );
    }
}
