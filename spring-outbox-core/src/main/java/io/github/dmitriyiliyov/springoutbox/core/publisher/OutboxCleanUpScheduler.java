package io.github.dmitriyiliyov.springoutbox.core.publisher;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduleStrategy;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;

public final class OutboxCleanUpScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanUpScheduler.class);

    private final OutboxPropertiesHolder.CleanUpPropertiesHolder properties;
    private final OutboxScheduleStrategy strategy;
    private final Clock clock;
    private final OutboxManager manager;

    public OutboxCleanUpScheduler(OutboxPropertiesHolder.CleanUpPropertiesHolder cleanupProperties,
                                  OutboxScheduleStrategy strategy,
                                  Clock clock,
                                  OutboxManager manager) {
        this.properties = cleanupProperties;
        this.strategy = strategy;
        this.clock = clock;
        this.manager = manager;
    }

    @Override
    public void schedule() {
        strategy.scheduleExecution(() -> {
            int batchSize = properties.getBatchSize();
            int deletedCount = 0;
            try {
                log.debug("Start clean up processed events");
                Instant threshold = clock.instant().minus(properties.getTtl());
                deletedCount = manager.deleteProcessedBatch(threshold, batchSize);
            } catch (Exception e) {
                log.error("Error process clean up outbox", e);
            }
            return deletedCount == batchSize;
        });
    }
}
