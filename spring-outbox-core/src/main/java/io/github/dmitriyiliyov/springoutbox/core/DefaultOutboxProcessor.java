package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.domain.SenderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DefaultOutboxProcessor implements OutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxProcessor.class);

    protected final OutboxManager manager;
    protected final OutboxSender sender;

    public DefaultOutboxProcessor(OutboxManager manager, OutboxSender sender) {
        this.manager = manager;
        this.sender = sender;
    }

    @Override
    public void process(OutboxProperties.EventProperties properties) {
        Objects.requireNonNull(properties, "properties cannot be null");
        List<OutboxEvent> events = manager.loadBatch(properties.getEventType(), properties.getBatchSize());
        if (events == null) {
            log.warn("Outbox events is unexpectedly null, for eventType={}; timestamp={}",
                    properties.getEventType(), Instant.now());
            return;
        }
        if (events.isEmpty()) {
            log.info("Outbox events is empty, for eventType={}; timestamp={}", properties.getEventType(), Instant.now());
            return;
        }
        SenderResult result;
        try {
            result = sender.sendEvents(properties.getTopic(), events);
        } catch (Exception e) {
            log.error("Error when processing batch {} events with size={}", properties.getEventType(), properties.getBatchSize());
            result = new SenderResult(
                    null,
                    events.stream()
                            .map(OutboxEvent::getId)
                            .collect(Collectors.toSet())
            );
        }
        manager.finalizeBatch(
                events,
                result.processedIds(),
                result.failedIds(),
                properties.getMaxRetries(),
                retryCount -> Instant.now().plusSeconds(
                        (long) Math.pow(properties.backoffMultiplier(), retryCount) * properties.backoffDelay()
                )
        );
    }
}
