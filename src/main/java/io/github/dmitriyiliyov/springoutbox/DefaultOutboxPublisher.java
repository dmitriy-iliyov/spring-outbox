package io.github.dmitriyiliyov.springoutbox;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisher;
import io.github.dmitriyiliyov.springoutbox.core.OutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.OutboxSerializer;
import io.github.dmitriyiliyov.springoutbox.core.config.OutboxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class DefaultOutboxPublisher implements OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxPublisher.class);

    private final OutboxProperties properties;
    private final OutboxSerializer serializer;
    private final OutboxRepository repository;

    public DefaultOutboxPublisher(OutboxProperties properties, OutboxSerializer serializer, OutboxRepository repository) {
        this.properties = properties;
        this.serializer = serializer;
        this.repository = repository;
    }

    @Override
    public <T> void publish(String eventType, T event) {
        validateEventType(eventType);
        Objects.requireNonNull(event, "event cannot be null");
        repository.save(serializer.serialize(eventType, event));
    }

    @Override
    public <T> void publish(String eventType, List<T> events) {
        validateEventType(eventType);
        Objects.requireNonNull(events, "events cannot be null");
        if (events.isEmpty()) {
            log.warn("Events is empty");
            return;
        }
        repository.saveBatch(serializer.serialize(eventType, events));
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
