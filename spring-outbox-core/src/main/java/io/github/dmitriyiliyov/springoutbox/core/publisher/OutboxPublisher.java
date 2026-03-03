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
     * <p>
     * The event is serialized and saved to the outbox table.
     * This method must be called within an active transaction.
     *
     * @param eventType The type of the event.
     * @param event     The event payload.
     * @param <T>       The type of the event payload.
     * @throws          IllegalArgumentException if the event type is not configured.
     */
    <T> void publish(String eventType, T event);

    /**
     * Publishes a list of events to the outbox.
     * <p>
     * All events in the list are saved in a single batch operation for efficiency.
     * This method must be called within an active transaction.
     *
     * @param eventType The type of the events.
     * @param events    A list of event payloads.
     * @param <T>       The type of the event payloads.
     * @throws          IllegalArgumentException if the event type is not configured or the list is empty.
     */
    <T> void publish(String eventType, List<T> events);
}
