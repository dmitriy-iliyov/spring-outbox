package io.github.dmitriyiliyov.springoutbox.core.publisher;

import java.util.List;

public interface OutboxPublisher {
    <T> void publish(String eventType, T event);
    <T> void publish(String eventType, List<T> events);
}