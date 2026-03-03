package io.github.dmitriyiliyov.springoutbox.aop;

import java.util.List;

/**
 * Represents a batch of outbox events to be published.
 * <p>
 * This record holds the event type and a list of event payloads.
 * It is typically used internally by the aspect to pass a batch of events to the listener.
 *
 * @param eventType The type of the events.
 * @param events    The list of event payloads.
 */
public record RowOutboxEvents(
        String eventType, 
        List<?> events
) { }
