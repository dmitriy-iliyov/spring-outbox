package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;

import java.util.List;

/**
 * Handler for events that have been moved to the Dead Letter Queue (DLQ).
 * <p>
 * This interface allows for custom logic to be executed when an event fails permanently and is moved to the DLQ.
 * Common use cases include logging, alerting, or sending notifications to an external system.
 */
public interface OutboxDlqHandler {

    /**
     * Handles a list of outbox events that have reached their maximum retry count.
     *
     * @param events The list of failed events to handle.
     */
    void handle(List<OutboxEvent> events);
}
