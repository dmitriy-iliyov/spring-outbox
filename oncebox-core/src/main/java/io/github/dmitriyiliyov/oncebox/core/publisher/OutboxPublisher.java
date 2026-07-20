package io.github.dmitriyiliyov.oncebox.core.publisher;

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
     * Method must be called within an active transaction.
     *
     * @param eventType the type of the event.
     * @param event     the event payload.
     * @param <T>       the type of the event payload.
     * @throws          IllegalArgumentException if the event type is not configured or the list is empty.
     */
    <T> void publish(String eventType, T event);

    /**
     * Publishes a list of events to the outbox.
     * <p>
     * Method must be called within an active transaction.
     * Does nothing if the list is empty.
     *
     * @param eventType the type of the events.
     * @param events    a list of event payloads.
     * @param <T>       the type of the event payloads.
     * @throws          IllegalArgumentException if the event type is not configured or the list is empty.
     */
    <T> void publish(String eventType, List<T> events);
}
