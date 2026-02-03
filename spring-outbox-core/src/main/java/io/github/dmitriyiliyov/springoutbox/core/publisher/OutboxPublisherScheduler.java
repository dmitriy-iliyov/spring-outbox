package io.github.dmitriyiliyov.springoutbox.core.publisher;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class OutboxPublisherScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherScheduler.class);

    private final OutboxPublisherPropertiesHolder.EventPropertiesHolder eventProperties;
    private final ScheduledExecutorService executor;
    private final OutboxProcessor processor;

    public OutboxPublisherScheduler(OutboxPublisherPropertiesHolder.EventPropertiesHolder eventProperties, ScheduledExecutorService executor,
                                    OutboxProcessor processor) {
        this.eventProperties = eventProperties;
        this.executor = executor;
        this.processor = processor;
    }

    @Override
    public void schedule() {
        executor.scheduleWithFixedDelay(
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
