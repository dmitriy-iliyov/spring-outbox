package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class OutboxCleanUpScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanUpScheduler.class);

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
                    try {
                        Instant threshold = Instant.now().minus(cleanupProperties.ttl());
                        manager.deleteProcessedBatch(threshold, cleanupProperties.batchSize());
                    } catch (Exception e) {
                        log.error("Error process clean up outbox", e);
                    }
                },
                cleanupProperties.initialDelay().getSeconds(),
                cleanupProperties.fixedDelay().getSeconds(),
                TimeUnit.SECONDS
        );
    }
}
