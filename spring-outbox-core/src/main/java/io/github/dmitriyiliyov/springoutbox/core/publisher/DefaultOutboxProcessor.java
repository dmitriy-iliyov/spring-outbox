package io.github.dmitriyiliyov.springoutbox.core.publisher;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.SenderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DefaultOutboxProcessor implements OutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxProcessor.class);

    protected final OutboxManager manager;
    protected final OutboxSender sender;
    protected final Clock clock;

    public DefaultOutboxProcessor(OutboxManager manager, OutboxSender sender, Clock clock) {
        this.manager = manager;
        this.sender = sender;
        this.clock = clock;
    }

    @Override
    public int process(OutboxPublisherPropertiesHolder.EventPropertiesHolder properties) {
        Objects.requireNonNull(properties, "properties cannot be null");
        List<OutboxEvent> events = manager.loadBatch(properties.getEventType(), properties.getBatchSize());
        if (events == null) {
            log.warn("Outbox events is unexpectedly null, for eventType={}", properties.getEventType());
            return 0;
        }
        if (events.isEmpty()) {
            log.info("Outbox events is empty, for eventType={}", properties.getEventType());
            return 0;
        }
        SenderResult result;
        try {
            result = sender.sendEvents(properties.getTopic(), events);
        } catch (Exception e) {
            log.error("Error when processing batch {} events with size={}", properties.getEventType(), properties.getBatchSize(), e);
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
                retryCount -> {
                    double currentMultiplier = Math.pow(properties.backoffMultiplier(), retryCount);
                    return clock.instant()
                            .plusSeconds((long) currentMultiplier * properties.backoffDelay());
                }
        );
        return events.size();
    }
}
