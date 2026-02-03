package io.github.dmitriyiliyov.springoutbox.core.publisher;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.SenderResult;

import java.util.List;

public interface OutboxSender {
    /**
     * Sends a batch of outbox events to a logical channel.
     *
     * @param topic logical channel for events/messages; can represent a Kafka topic, RabbitMQ exchange, or any other event stream
     * @param events list of events to send; must not be null, can be empty
     * @return DTO containing ids of events successfully sent and ids of events that failed to send
     */
    SenderResult sendEvents(String topic, List<OutboxEvent> events);
}
