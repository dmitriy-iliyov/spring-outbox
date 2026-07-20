package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.polling.OutboxScheduleStrategyListener;
import io.github.dmitriyiliyov.oncebox.metrics.MetricsOutboxScheduleStrategyListener;
import io.micrometer.core.instrument.MeterRegistry;

public class MetricsOutboxScheduleStrategyListenerSupplier implements OutboxScheduleStrategyListenerSupplier {

    private final MeterRegistry registry;

    public MetricsOutboxScheduleStrategyListenerSupplier(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public OutboxScheduleStrategyListener supply(String taskType) {
        return new MetricsOutboxScheduleStrategyListener(taskType, registry);
    }
}
