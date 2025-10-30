package io.github.dmitriyiliyov.springoutbox.core;


import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class OutboxStuckEventRecoveryScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxStuckEventRecoveryScheduler.class);

    private final OutboxProperties.StuckEventRecoveryProperties properties;
    private final ScheduledExecutorService executor;
    private final OutboxManager manager;

    public OutboxStuckEventRecoveryScheduler(OutboxProperties.StuckEventRecoveryProperties properties, ScheduledExecutorService executor, OutboxManager manager) {
        this.properties = properties;
        this.executor = executor;
        this.manager = manager;
    }

    @Override
    public void schedule() {
        executor.scheduleWithFixedDelay(
                () -> {
                    try {
                        manager.recoverStuckBatch(properties.getBatchSize());
                    } catch (Exception e) {
                        log.error("Error process recover stuck outbox events", e);
                    }
                },
                properties.getInitialDelay().toSeconds(),
                properties.getFixedDelay().toSeconds(),
                TimeUnit.SECONDS
        );
    }
}
