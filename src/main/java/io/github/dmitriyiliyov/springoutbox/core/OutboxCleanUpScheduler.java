package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;

import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OutboxCleanUpScheduler implements OutboxScheduler {

    private final OutboxProperties.CleanUpProperties cleanupProperties;
    private final OutboxProcessor processor;
    private final ScheduledExecutorService executor;

    public OutboxCleanUpScheduler(OutboxProperties.CleanUpProperties cleanupProperties, OutboxProcessor processor,
                                  ScheduledExecutorService executor) {
        this.cleanupProperties = cleanupProperties;
        this.processor = processor;
        this.executor = executor;
    }

    @Override
    public void schedule() {
        executor.scheduleWithFixedDelay(
                () -> {
                    Instant threshold = Instant.now().minus(cleanupProperties.getTtl());
                    processor.cleanUpBatch(threshold, cleanupProperties.getBatchSize());
                },
                cleanupProperties.getInitialDelay().getSeconds(),
                cleanupProperties.getFixedDelay().getSeconds(),
                TimeUnit.SECONDS
        );
    }
}
