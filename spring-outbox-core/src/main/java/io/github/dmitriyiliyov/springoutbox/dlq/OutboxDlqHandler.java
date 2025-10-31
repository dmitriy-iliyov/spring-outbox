package io.github.dmitriyiliyov.springoutbox.dlq;

import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;

import java.util.List;

public interface OutboxDlqHandler {
    void handle(List<OutboxEvent> events);
}
