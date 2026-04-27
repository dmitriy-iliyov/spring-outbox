package io.github.dmitriyiliyov.springoutbox.metrics;

import io.github.dmitriyiliyov.springoutbox.core.ContinuableTask;
import io.github.dmitriyiliyov.springoutbox.core.ContinuableTaskDecorator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

public class ContinuableTaskTimeMeasureDecorator implements ContinuableTaskDecorator {

    private final MeterRegistry registry;
    private final Timer timer;

    public ContinuableTaskTimeMeasureDecorator(MeterRegistry registry, String taskType) {
        this.registry = registry;
        this.timer = registry.timer("outbox_task_processing_duration", "task_type", taskType);
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
