package io.github.dmitriyiliyov.springoutbox.core.aop;

public record RowOutboxEvent(
        String eventType,
        Object event
) { }
