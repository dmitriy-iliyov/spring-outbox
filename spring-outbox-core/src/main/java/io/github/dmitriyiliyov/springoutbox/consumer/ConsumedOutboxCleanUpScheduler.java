package io.github.dmitriyiliyov.springoutbox.consumer;

import io.github.dmitriyiliyov.springoutbox.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ConsumedOutboxCleanUpScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(ConsumedOutboxCleanUpScheduler.class);

    private final OutboxProperties.CleanUpProperties properties;
    private final ScheduledExecutorService executor;
    private final ConsumedOutboxManager manager;

    public ConsumedOutboxCleanUpScheduler(OutboxProperties.CleanUpProperties properties, ScheduledExecutorService executor,
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
