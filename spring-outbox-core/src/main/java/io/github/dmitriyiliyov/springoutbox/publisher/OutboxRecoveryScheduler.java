package io.github.dmitriyiliyov.springoutbox.publisher;


import io.github.dmitriyiliyov.springoutbox.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class OutboxRecoveryScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRecoveryScheduler.class);

    private final OutboxPublisherProperties.StuckRecoveryProperties properties;
    private final ScheduledExecutorService executor;
    private final OutboxManager manager;

    public OutboxRecoveryScheduler(OutboxPublisherProperties.StuckRecoveryProperties properties, ScheduledExecutorService executor, OutboxManager manager) {
        this.properties = properties;
        this.executor = executor;
        this.manager = manager;
    }

    @Override
    public void schedule() {
        executor.scheduleWithFixedDelay(
                () -> {
                    try {
                        log.debug("Start recovering stuck outbox events");
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
