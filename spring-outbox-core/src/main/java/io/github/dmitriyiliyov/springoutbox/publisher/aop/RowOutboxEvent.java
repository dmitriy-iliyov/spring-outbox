package io.github.dmitriyiliyov.springoutbox.publisher.aop;

public record RowOutboxEvent(
        String eventType,
        Object event
) { }
