package io.github.dmitriyiliyov.springoutbox.core.publisher;

import java.util.List;

/**
 * Provides a high-level API for publishing events to the outbox.
 * <p>
 * This is the primary entry point for application code to manually send events.
 */
public interface OutboxPublisher {

    /**
     * Publishes a single event to the outbox.
     *
     * @param eventType The type of the event.
     * @param event     The event payload.
     * @param <T>       The type of the event payload.
     */
    <T> void publish(String eventType, T event);

    /**
     * Publishes a list of events to the outbox.
     *
     * @param eventType The type of the events.
     * @param events    A list of event payloads.
     * @param <T>       The type of the event payloads.
     */
    <T> void publish(String eventType, List<T> events);
}
