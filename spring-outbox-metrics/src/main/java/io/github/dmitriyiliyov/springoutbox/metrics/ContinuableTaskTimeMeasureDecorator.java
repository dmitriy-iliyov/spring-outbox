package io.github.dmitriyiliyov.springoutbox.metrics;

import io.github.dmitriyiliyov.springoutbox.core.ContinuableTask;
import io.github.dmitriyiliyov.springoutbox.core.ContinuableTaskDecorator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.Objects;

public class ContinuableTaskTimeMeasureDecorator implements ContinuableTaskDecorator {

    private final MeterRegistry registry;
    private final Timer timer;

    public ContinuableTaskTimeMeasureDecorator(MeterRegistry registry, String taskType) {
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
        this.timer = registry.timer("outbox_task_processing_duration", "task_type", Objects.requireNonNull(taskType, "taskType cannot be null"));
    }

    @Override
    public ContinuableTask decorate(ContinuableTask task) {
        return () -> {
            Timer.Sample sample = Timer.start(registry);
            try {
                return task.run();
            } finally {
                sample.stop(timer);
            }
        };
    }
}
