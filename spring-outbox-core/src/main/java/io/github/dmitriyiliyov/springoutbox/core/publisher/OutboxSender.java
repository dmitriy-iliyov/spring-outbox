package io.github.dmitriyiliyov.springoutbox.core.publisher;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.SenderResult;

import java.util.List;

/**
 * Abstraction for sending outbox events to a message broker.
 * <p>
 * Implementations of this interface are responsible for publishing events to a specific messaging system (e.g., Kafka, RabbitMQ).
 * This is a key extension point for supporting new message brokers.
 */
public interface OutboxSender {

    /**
     * Sends a batch of outbox events to a logical channel.
     * <p>
     * The implementation should handle the actual transmission to the broker.
     * It must return a {@link SenderResult} containing the IDs of events that were successfully sent
     * and those that failed. This allows for partial success handling.
     *
     * @param topic  Logical channel for events; represents a Kafka topic, RabbitMQ exchange, or any other event stream destination.
     * @param events List of events to send.
     * @return       A {@link SenderResult} containing sets of processed and failed event IDs.
     */
    SenderResult sendEvents(String topic, List<OutboxEvent> events);
}
