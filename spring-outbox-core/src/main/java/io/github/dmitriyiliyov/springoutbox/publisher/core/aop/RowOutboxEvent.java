package io.github.dmitriyiliyov.springoutbox.publisher.core.aop;

public record RowOutboxEvent(
        String eventType,
        Object event
) { }
