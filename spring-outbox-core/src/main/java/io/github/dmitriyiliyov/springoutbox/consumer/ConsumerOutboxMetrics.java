package io.github.dmitriyiliyov.springoutbox.consumer;

import io.github.dmitriyiliyov.springoutbox.OutboxMetrics;
import io.micrometer.core.instrument.MeterRegistry;

public final class ConsumerOutboxMetrics implements OutboxMetrics {

    private final ConsumedOutboxManager manager;
    private final MeterRegistry registry;

    public ConsumerOutboxMetrics(ConsumedOutboxManager manager, MeterRegistry registry) {
        this.manager = manager;
        this.registry = registry;
    }

    @Override
    public void register() {

    }
}
