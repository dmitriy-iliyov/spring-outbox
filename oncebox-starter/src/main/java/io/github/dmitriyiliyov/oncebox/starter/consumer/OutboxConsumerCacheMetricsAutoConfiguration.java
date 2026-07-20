package io.github.dmitriyiliyov.oncebox.starter.consumer;

import io.github.dmitriyiliyov.oncebox.consumer.cache.ConsumedOutboxCache;
import io.github.dmitriyiliyov.oncebox.consumer.cache.ConsumedOutboxCacheListener;
import io.github.dmitriyiliyov.oncebox.metrics.OutboxMetrics;
import io.github.dmitriyiliyov.oncebox.metrics.consumer.MetricsConsumedOutboxCacheListener;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        prefix = "oncebox.consumer",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnClass({ConsumedOutboxCache.class, OutboxMetrics.class, MeterRegistry.class})
@ConditionalOnProperty(
        prefix = "oncebox.consumer.cache",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnProperty(
        prefix = "oncebox.consumer.metrics",
        name = "enabled",
        havingValue = "true"
)
public class OutboxConsumerCacheMetricsAutoConfiguration {

    @Bean
    public ConsumedOutboxCacheListener metricsConsumedOutboxCacheListener(MeterRegistry registry) {
        return new MetricsConsumedOutboxCacheListener(registry);
    }
}
