package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class OutboxEventScheduler implements OutboxScheduler {

    private final OutboxProperties.EventProperties eventProperties;
    private final ScheduledExecutorService executor;
    private final OutboxProcessor processor;

    public OutboxEventScheduler(OutboxProperties.EventProperties eventProperties, ScheduledExecutorService executor,
                                OutboxProcessor processor) {
        this.eventProperties = eventProperties;
        this.executor = executor;
        this.processor = processor;
    }

    /**
     * Periodically loads and sends outbox events for a specific event type.
     * Uses {@link ScheduledExecutorService#scheduleAtFixedRate} to trigger execution at a fixed rate.
     * <p>
     *     This is important in the Transactional Outbox pattern because messages must be sent regularly and without gaps
     * so that the external system (e.g., Kafka) receives events at the required rate.
     * Using {@link ScheduledExecutorService#scheduleWithFixedDelay} is also not ideal here, because the interval
     * between starts will depend on the execution time of the previous batch, which can disrupt the regularity of publishing.
     */
    @Override
    public void schedule() {
        executor.scheduleAtFixedRate(
                () -> processor.process(eventProperties),
                eventProperties.initialDelay().getSeconds(),
                eventProperties.fixedDelay().getSeconds(),
                TimeUnit.SECONDS
        );
    }
}
