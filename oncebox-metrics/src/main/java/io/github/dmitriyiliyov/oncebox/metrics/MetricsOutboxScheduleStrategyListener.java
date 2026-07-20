package io.github.dmitriyiliyov.oncebox.metrics;

import io.github.dmitriyiliyov.oncebox.core.polling.OutboxScheduleStrategyListener;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Objects;

public class MetricsOutboxScheduleStrategyListener implements OutboxScheduleStrategyListener {

    private final Counter startedCounter;
    private final Counter skippedCounter;
    private final Counter succeededCounter;
    private final Counter failedCounter;
    private volatile long currentDelay;

    public MetricsOutboxScheduleStrategyListener(String taskType, MeterRegistry registry) {
        Objects.requireNonNull(taskType, "taskType cannot be null");
        if (taskType.isBlank()) {
            throw new IllegalArgumentException("taskType cannot be empty or blank");
        }
        Objects.requireNonNull(registry, "registry cannot be null");
        this.startedCounter = Counter.builder("outbox_started_tasks")
                .description("Total number of started tasks")
                .tag("task_type", taskType)
                .register(registry);
        this.skippedCounter =  Counter.builder("outbox_skipped_tasks")
                .description("Total number of skipped tasks")
                .tag("task_type", taskType)
                .register(registry);
        this.succeededCounter =  Counter.builder("outbox_succeeded_tasks")
                .description("Total number of succeeded tasks")
                .tag("task_type", taskType)
                .register(registry);
        this.failedCounter =  Counter.builder("outbox_failed_tasks")
                .description("Total number of failed tasks")
                .tag("task_type", taskType)
                .register(registry);
        Gauge.builder("outbox_polling_delay", () -> currentDelay)
                .description("Current polling delay between task execution")
                .tag("task_type", taskType)
                .baseUnit("milliseconds")
                .register(registry);
    }

    @Override
    public void onExecutionStarted() {
        startedCounter.increment();
    }

    @Override
    public void onExecutionSkipped() {
        skippedCounter.increment();
    }

    @Override
    public void onExecutionSucceeded() {
        succeededCounter.increment();
    }

    @Override
    public void onExecutionFailed() {
        failedCounter.increment();
    }

    @Override
    public synchronized void onDelayChanged(long delay) {
        currentDelay = delay;
    }
}
