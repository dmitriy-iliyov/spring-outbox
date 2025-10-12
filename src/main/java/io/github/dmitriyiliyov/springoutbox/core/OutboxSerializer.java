package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;

import java.util.List;

public interface OutboxSerializer {
    <T> OutboxEvent serialize(String eventType, T event);
    <T> List<OutboxEvent> serialize(String eventType, List<T> rowEvents);
}
