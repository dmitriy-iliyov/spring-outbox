package io.github.dmitriyiliyov.springoutbox.publisher;

import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxEvent;

import java.util.List;

public interface OutboxSerializer {
    <T> OutboxEvent serialize(String eventType, T event);
    <T> List<OutboxEvent> serialize(String eventType, List<T> rowEvents);
}
