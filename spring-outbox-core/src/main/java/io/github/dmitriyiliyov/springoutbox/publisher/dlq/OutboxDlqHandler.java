package io.github.dmitriyiliyov.springoutbox.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.publisher.core.domain.OutboxEvent;

import java.util.List;

public interface OutboxDlqHandler {
    void handle(List<OutboxEvent> events);
}
