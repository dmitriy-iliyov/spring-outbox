package io.github.dmitriyiliyov.springoutbox.publisher.core;

import io.github.dmitriyiliyov.springoutbox.publisher.core.domain.OutboxEvent;

import java.util.List;

public interface OutboxSerializer {
    <T> OutboxEvent serialize(String eventType, T event);
    <T> List<OutboxEvent> serialize(String eventType, List<T> rowEvents);
}
