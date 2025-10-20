package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;

import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class OutboxCleanUpScheduler implements OutboxScheduler {

    private final ScheduledExecutorService executor;
    private final OutboxProperties.CleanUpProperties cleanupProperties;
    private final OutboxManager manager;

    public OutboxCleanUpScheduler(OutboxProperties.CleanUpProperties cleanupProperties, ScheduledExecutorService executor,
                                  OutboxManager manager) {
        this.cleanupProperties = cleanupProperties;
        this.executor = executor;
        this.manager = manager;
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
                    Instant threshold = Instant.now().minus(cleanupProperties.ttl());
                    manager.deleteBatch(threshold, cleanupProperties.batchSize());
                },
                cleanupProperties.initialDelay().getSeconds(),
                cleanupProperties.fixedDelay().getSeconds(),
                TimeUnit.SECONDS
        );
    }
}
