package io.github.dmitriyiliyov.springoutbox.core.publisher;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.SenderResult;

import java.util.List;

/**
 * Abstraction for sending outbox events to a message broker.
 * <p>
 * Implementations of this interface are responsible for publishing events to a specific messaging system (e.g., Kafka, RabbitMQ).
 */
public interface OutboxSender {

    /**
     * Sends a batch of outbox events to a logical channel.
     *
     * @param topic  Logical channel for events; represent a Kafka topic, RabbitMQ exchange, or any other event stream
     * @param events List of events to send
     * @return       Events ids that successfully and failed send
     */
    SenderResult sendEvents(String topic, List<OutboxEvent> events);
}
