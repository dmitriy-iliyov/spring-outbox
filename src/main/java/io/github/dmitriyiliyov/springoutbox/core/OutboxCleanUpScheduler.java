package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;

import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OutboxCleanUpScheduler implements OutboxScheduler {

    private final OutboxProperties.CleanUpProperties cleanupProperties;
    private final OutboxManager manager;
    private final ScheduledExecutorService executor;

    public OutboxCleanUpScheduler(OutboxProperties.CleanUpProperties cleanupProperties, OutboxManager manager,
                                  ScheduledExecutorService executor) {
        this.cleanupProperties = cleanupProperties;
        this.manager = manager;
        this.executor = executor;
    }

    /**
     * Schedules periodic cleanup of old outbox events.
     * Uses {@link ScheduledExecutorService#scheduleWithFixedDelay} to ensure
     * that the next execution starts only after the previous one has finished.
     * <p>
     *     This is suitable because cleanup does not need to happen at a strict interval,
     * reduces load on the database (prevents running multiple large delete requests simultaneously),
     * and prevents unnecessary thread usage.
     */
    @Override
    public void schedule() {
        executor.scheduleWithFixedDelay(
                () -> {
                    Instant threshold = Instant.now().minus(cleanupProperties.getTtl());
                    manager.cleanUpBatch(threshold, cleanupProperties.getBatchSize());
                },
                cleanupProperties.getInitialDelay().getSeconds(),
                cleanupProperties.getFixedDelay().getSeconds(),
                TimeUnit.SECONDS
        );
    }
}
