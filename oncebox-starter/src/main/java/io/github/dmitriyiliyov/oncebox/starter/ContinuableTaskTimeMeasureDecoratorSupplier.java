package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.oncebox.metrics.ContinuableTaskTimeMeasureDecorator;
import io.micrometer.core.instrument.MeterRegistry;

public class ContinuableTaskTimeMeasureDecoratorSupplier implements ContinuableTaskDecoratorSupplier {

    private final MeterRegistry registry;

    public ContinuableTaskTimeMeasureDecoratorSupplier(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public ContinuableTaskDecorator supply(String taskType) {
        return new ContinuableTaskTimeMeasureDecorator(registry, taskType);
    }
}
