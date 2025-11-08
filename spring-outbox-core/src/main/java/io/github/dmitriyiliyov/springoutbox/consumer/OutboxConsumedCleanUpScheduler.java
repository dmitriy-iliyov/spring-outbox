package io.github.dmitriyiliyov.springoutbox.consumer;

import io.github.dmitriyiliyov.springoutbox.config.CleanUpProperties;
import io.github.dmitriyiliyov.springoutbox.publisher.core.OutboxScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class OutboxConsumedCleanUpScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxConsumedCleanUpScheduler.class);

    private final CleanUpProperties properties;
    private final ScheduledExecutorService executor;
    private final OutboxManager manager;

    public OutboxConsumedCleanUpScheduler(CleanUpProperties properties, ScheduledExecutorService executor,
                                          OutboxManager manager) {
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
