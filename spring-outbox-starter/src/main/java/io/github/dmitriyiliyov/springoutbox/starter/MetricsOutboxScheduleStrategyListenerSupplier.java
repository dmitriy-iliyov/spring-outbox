package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduleStrategyListener;
import io.github.dmitriyiliyov.springoutbox.metrics.MetricsOutboxScheduleStrategyListener;
import io.micrometer.core.instrument.MeterRegistry;

public final class MetricsOutboxScheduleStrategyListenerSupplier implements OutboxScheduleStrategyListenerSupplier {

    private final MeterRegistry registry;

    public MetricsOutboxScheduleStrategyListenerSupplier(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public OutboxScheduleStrategyListener supply(String taskType) {
        return new MetricsOutboxScheduleStrategyListener(taskType, registry);
    }
}
