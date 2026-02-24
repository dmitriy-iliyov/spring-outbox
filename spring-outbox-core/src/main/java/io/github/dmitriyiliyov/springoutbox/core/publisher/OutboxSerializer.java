package io.github.dmitriyiliyov.springoutbox.core.publisher;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;

import java.util.List;

/**
 * Abstraction for serializing application-specific events into a generic OutboxEvent format.
 * <p>
 * Implementations are responsible for converting event payloads into a storable representation.
 */
public interface OutboxSerializer {

    /**
     * Serializes a single application-specific event into an OutboxEvent.
     *
     * @param eventType The type of the event.
     * @param event     The application-specific event object.
     * @param <T>       The type of the application-specific event object.
     * @return          The serialized OutboxEvent.
     */
    <T> OutboxEvent serialize(String eventType, T event);

    /**
     * Serializes a list of application-specific events into a list of OutboxEvents.
     *
     * @param eventType The type of the events.
     * @param rowEvents A list of application-specific event objects.
     * @param <T>       The type of the application-specific event objects.
     * @return          A list of serialized OutboxEvents.
     */
    <T> List<OutboxEvent> serialize(String eventType, List<T> rowEvents);
}
