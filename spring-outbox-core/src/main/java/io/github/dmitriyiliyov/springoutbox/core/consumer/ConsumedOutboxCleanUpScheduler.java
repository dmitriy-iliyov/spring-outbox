package io.github.dmitriyiliyov.springoutbox.core.consumer;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ConsumedOutboxCleanUpScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(ConsumedOutboxCleanUpScheduler.class);

    private final OutboxPropertiesHolder.CleanUpPropertiesHolder properties;
    private final ScheduledExecutorService executor;
    private final ConsumedOutboxManager manager;

    public ConsumedOutboxCleanUpScheduler(OutboxPropertiesHolder.CleanUpPropertiesHolder properties, ScheduledExecutorService executor,
                                          ConsumedOutboxManager manager) {
        this.properties = properties;
        this.executor = executor;
        this.manager = manager;
    }

    @Override
    public void schedule() {
        executor.scheduleWithFixedDelay(
                () -> {
                    try {
                        manager.cleanBatchByTtl(properties.getTtl(), properties.getBatchSize());
                    } catch (Exception e) {
                        log.error("Error when cleanup consumed outbox events", e);
                    }
                },
                properties.getInitialDelay().toSeconds(),
                properties.getFixedDelay().toSeconds(),
                TimeUnit.SECONDS
        );
    }
}
