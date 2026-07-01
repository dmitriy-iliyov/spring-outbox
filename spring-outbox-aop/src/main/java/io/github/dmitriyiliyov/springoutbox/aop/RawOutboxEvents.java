package io.github.dmitriyiliyov.springoutbox.aop;

import java.util.List;

/**
 * Represents a batch of outbox events to be published.
 */
public record RawOutboxEvents(
        String eventType, 
        List<?> events
) { }
