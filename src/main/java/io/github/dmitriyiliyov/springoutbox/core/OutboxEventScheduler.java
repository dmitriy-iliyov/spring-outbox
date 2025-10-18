package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.domain.SenderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class OutboxEventScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventScheduler.class);
    private final OutboxProperties.EventProperties eventProperties;
    private final OutboxProcessor processor;
    private final OutboxSender sender;
    private final ScheduledExecutorService executor;

    public OutboxEventScheduler(OutboxProperties.EventProperties eventProperties, OutboxProcessor processor,
                                OutboxSender sender, ScheduledExecutorService executor) {
        this.eventProperties = eventProperties;
        this.processor = processor;
        this.sender = sender;
        this.executor = executor;
    }

    @Override
    public void schedule() {
        executor.scheduleAtFixedRate(
                () -> {
                    List<OutboxEvent> events = processor.loadBatch(eventProperties.eventType(), eventProperties.batchSize());
                    SenderResult result;
                    try {
                        result = sender.sendEvents(eventProperties.topic(), events);
                    } catch (Exception e) {
                        log.error("Error when processing batch {} events with size={}", eventProperties.eventType(), eventProperties.batchSize());
                        result = new SenderResult(
                                null,
                                events.stream()
                                        .map(OutboxEvent::getId)
                                        .collect(Collectors.toSet())
                        );
                    }
                    processor.finalizeBatch(result.processedIds(), result.failedIds(), eventProperties.maxRetries());
                },
                eventProperties.initialDelay().getSeconds(),
                eventProperties.fixedDelay().getSeconds(),
                TimeUnit.SECONDS
        );
    }
}
