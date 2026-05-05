package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.springoutbox.metrics.ContinuableTaskTimeMeasureDecorator;
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
