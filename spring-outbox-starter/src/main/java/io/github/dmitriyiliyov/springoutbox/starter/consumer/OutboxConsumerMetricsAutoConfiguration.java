package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxManager;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.metrics.consumer.ConsumedOutboxManagerMetricsDecorator;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

@Configuration
@ConditionalOnProperty(
        prefix = "outbox.consumer",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnClass({MeterRegistry.class, OutboxMetrics.class})
@ConditionalOnProperty(
        prefix = "outbox.consumer.metrics",
        name = "enabled",
        havingValue = "true"
)
public class OutboxConsumerMetricsAutoConfiguration {

    @Bean
    @Primary
    public ConsumedOutboxManager consumedOutboxManagerMetricsDecorator(
            @Qualifier("consumedOutboxManager") ConsumedOutboxManager consumedOutboxManager,
            MeterRegistry registry
    ) {
        return new ConsumedOutboxManagerMetricsDecorator(registry, consumedOutboxManager);
    }

    @Bean
    @Order(2)
    public OutboxIdempotentConsumerDecoratorSupplier outboxIdempotentConsumerMetricsDecoratorSupplier(MeterRegistry registry) {
        return new OutboxIdempotentConsumerMetricsDecoratorSupplier(registry);
    }
}
