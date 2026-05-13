package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.AbstractOutboxIdempotentConsumerDecorator;
import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.springoutbox.metrics.consumer.OutboxIdempotentConsumerMetricsDecorator;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Objects;

public class OutboxIdempotentConsumerMetricsDecoratorSupplier implements OutboxIdempotentConsumerDecoratorSupplier {

    private final MeterRegistry registry;

    public OutboxIdempotentConsumerMetricsDecoratorSupplier(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
    }

    @Override
    public AbstractOutboxIdempotentConsumerDecorator supply(OutboxIdempotentConsumer consumer) {
        return new OutboxIdempotentConsumerMetricsDecorator(
                registry,
                Objects.requireNonNull(consumer, "consumer cannot be null")
        );
    }
}
