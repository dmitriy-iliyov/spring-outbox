package io.github.dmitriyiliyov.springoutbox.publisher.core;

import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class OutboxPublisherScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherScheduler.class);

    private final OutboxPublisherProperties.EventProperties eventProperties;
    private final ScheduledExecutorService executor;
    private final OutboxProcessor processor;

    public OutboxPublisherScheduler(OutboxPublisherProperties.EventProperties eventProperties, ScheduledExecutorService executor,
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
                () -> {
                    try {
                        log.debug("Start processing {} outbox events", eventProperties.getEventType());
                        processor.process(eventProperties);
                    } catch (Exception e) {
                        log.error("Error process outbox events for type={}", eventProperties.getEventType(), e);
                    }
                },
                eventProperties.getInitialDelay().toSeconds(),
                eventProperties.getFixedDelay().toSeconds(),
                TimeUnit.SECONDS
        );
    }
}
