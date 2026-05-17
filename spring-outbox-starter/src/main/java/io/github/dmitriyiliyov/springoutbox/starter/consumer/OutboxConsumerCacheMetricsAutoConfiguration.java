package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.consumer.cache.ConsumedOutboxCache;
import io.github.dmitriyiliyov.springoutbox.consumer.cache.ConsumedOutboxCacheListener;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.metrics.consumer.MetricsConsumedOutboxCacheListener;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        prefix = "outbox.consumer",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnClass({ConsumedOutboxCache.class, OutboxMetrics.class, MeterRegistry.class})
@ConditionalOnProperty(
        prefix = "outbox.consumer.cache",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnProperty(
        prefix = "outbox.consumer.metrics",
        name = "enabled",
        havingValue = "true"
)
public class OutboxConsumerCacheMetricsAutoConfiguration {

    @Bean
    public ConsumedOutboxCacheListener metricsConsumedOutboxCacheListener(MeterRegistry registry) {
        return new MetricsConsumedOutboxCacheListener(registry);
    }
}
