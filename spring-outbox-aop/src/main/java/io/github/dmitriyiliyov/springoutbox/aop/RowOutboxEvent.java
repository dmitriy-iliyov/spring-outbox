package io.github.dmitriyiliyov.springoutbox.aop;

/**
 * Represents a single outbox event to be published.
 */
public record RowOutboxEvent(
        String eventType,
        Object event
) { }
