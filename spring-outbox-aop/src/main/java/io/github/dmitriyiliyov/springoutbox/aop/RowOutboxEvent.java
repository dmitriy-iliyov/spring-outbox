package io.github.dmitriyiliyov.springoutbox.aop;

/**
 * Represents a single outbox event to be published.
 * <p>
 * This record holds the event type and the event payload.
 * It is typically used internally by the aspect to pass event data to the listener.
 *
 * @param eventType The type of the event.
 * @param event     The event payload.
 */
public record RowOutboxEvent(
        String eventType,
        Object event
) { }
