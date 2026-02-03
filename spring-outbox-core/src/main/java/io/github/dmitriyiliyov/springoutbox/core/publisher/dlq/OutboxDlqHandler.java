package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;

import java.util.List;

public interface OutboxDlqHandler {
    void handle(List<OutboxEvent> events);
}
