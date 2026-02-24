package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;

import java.util.List;

/**
 * Abstraction specify the action when events that have been moved to the DLQ.
 */
public interface OutboxDlqHandler {
    /**
     * Handles a list of outbox events that have reached their maximum retry count.
     *
     * @param events The list of failed events to handle.
     */
    void handle(List<OutboxEvent> events);
}
