package io.github.dmitriyiliyov.springoutbox.core.publisher;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class OutboxCleanUpScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanUpScheduler.class);

    private final ScheduledExecutorService executor;
    private final OutboxPropertiesHolder.CleanUpPropertiesHolder cleanupProperties;
    private final OutboxManager manager;
    private final Clock clock;

    public OutboxCleanUpScheduler(OutboxPropertiesHolder.CleanUpPropertiesHolder cleanupProperties,
                                  ScheduledExecutorService executor,
                                  OutboxManager manager,
                                  Clock clock) {
        this.cleanupProperties = cleanupProperties;
        this.executor = executor;
        this.manager = manager;
        this.clock = clock;
    }

    @Override
    public void schedule() {
        executor.scheduleWithFixedDelay(
                () -> {
                    try {
                        log.debug("Start clean up processed events");
                        Instant threshold = clock.instant().minus(cleanupProperties.getTtl());
                        manager.deleteProcessedBatch(threshold, cleanupProperties.getBatchSize());
                    } catch (Exception e) {
                        log.error("Error process clean up outbox", e);
                    }
                },
                cleanupProperties.getInitialDelay().toMillis(),
                cleanupProperties.getFixedDelay().toMillis(),
                TimeUnit.MILLISECONDS
        );
    }
}
