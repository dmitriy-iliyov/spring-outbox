package io.github.dmitriyiliyov.springoutbox.consumer;

import io.github.dmitriyiliyov.springoutbox.OutboxMetrics;
import io.micrometer.core.instrument.MeterRegistry;

public final class ConsumerOutboxMetrics implements OutboxMetrics {

    private final MeterRegistry registry;

    public ConsumerOutboxMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void register() {}
}
