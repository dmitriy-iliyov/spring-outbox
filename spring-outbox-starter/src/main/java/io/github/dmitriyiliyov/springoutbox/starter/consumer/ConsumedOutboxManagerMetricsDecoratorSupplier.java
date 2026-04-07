package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxManager;
import io.github.dmitriyiliyov.springoutbox.metrics.consumer.ConsumedOutboxManagerMetricsDecorator;
import io.micrometer.core.instrument.MeterRegistry;

public final class ConsumedOutboxManagerMetricsDecoratorSupplier implements ConsumedOutboxManagerDecoratorSupplier {

    private final MeterRegistry registry;

    public ConsumedOutboxManagerMetricsDecoratorSupplier(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public ConsumedOutboxManager supply(ConsumedOutboxManager manager) {
        return new ConsumedOutboxManagerMetricsDecorator(registry, manager);
    }
}
