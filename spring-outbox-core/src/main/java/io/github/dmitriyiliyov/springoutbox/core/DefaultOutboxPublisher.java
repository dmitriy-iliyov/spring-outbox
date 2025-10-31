package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class DefaultOutboxPublisher implements OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxPublisher.class);

    private final OutboxProperties properties;
    private final OutboxSerializer serializer;
    private final OutboxManager manager;

    public DefaultOutboxPublisher(OutboxProperties properties, OutboxSerializer serializer, OutboxManager manager) {
        this.properties = properties;
        this.serializer = serializer;
        this.manager = manager;
    }

    @Override
    public <T> void publish(String eventType, T event) {
        validateEventType(eventType);
        Objects.requireNonNull(event, "event cannot be null");
        manager.save(serializer.serialize(eventType, event));
    }

    @Override
    public <T> void publish(String eventType, List<T> events) {
        validateEventType(eventType);
        Objects.requireNonNull(events, "events cannot be null");
        if (events.isEmpty()) {
            log.warn("Published outbox event list is empty");
            return;
        }
        manager.saveBatch(serializer.serialize(eventType, events));
    }

    private void validateEventType(String eventType) {
        Objects.requireNonNull(eventType, "eventType cannot be null");
        if (eventType.isBlank()) {
            throw new IllegalArgumentException("eventType cannot be empty");
        }
        if (!properties.existEventType(eventType)) {
            throw new IllegalArgumentException("Non existed eventType");
        }
    }
}
