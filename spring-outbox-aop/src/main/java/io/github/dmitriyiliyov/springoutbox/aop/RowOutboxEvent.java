package io.github.dmitriyiliyov.springoutbox.aop;

public record RowOutboxEvent(
        String eventType,
        Object event
) { }
