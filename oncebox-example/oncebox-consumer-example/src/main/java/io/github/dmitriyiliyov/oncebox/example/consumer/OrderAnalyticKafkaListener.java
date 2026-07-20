package io.github.dmitriyiliyov.oncebox.example.consumer;

import io.github.dmitriyiliyov.oncebox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.example.shared.OrderDto;
import io.github.dmitriyiliyov.oncebox.messaging.OutboxHeadersUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.List;


@Profile("kafka")
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderAnalyticKafkaListener {

    private final OutboxIdempotentConsumer outboxConsumer;

    /**
     * Example of routing messages to different handlers based on the event type.
     * The event type is extracted from the Kafka record headers using OutboxHeaders.EVENT_TYPE.
     * This demonstrates how Outbox pattern headers can be used to dynamically dispatch messages.
     */
    @KafkaListener(topics = "orders", groupId = "analytics", containerFactory = "outboxKafkaListenerContainerFactory")
    public void listen(Message<?> message, Acknowledgment ack) {
        try {
            outboxConsumer.consume(
                    message,
                    OutboxHeadersUtils::extractId,
                    m -> log.info("Analytics receive '{}' event {}", OutboxHeadersUtils.extractEventType(m), m.getPayload())
            );
            ack.acknowledge();
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Demonstration of batch processing with KafkaListener.
     * This method shows how to consume a list of records in a single batch
     * and process them together for better throughput and efficiency.
     */
    @KafkaListener(topics = "orders.created", groupId = "analytics", containerFactory = "kafkaBatchListenerContainerFactory")
    public void listenBatch(List<Message<OrderDto>> messages, Acknowledgment ack) {
        try {
            outboxConsumer.consume(
                    messages,
                    OutboxHeadersUtils::extractId,
                    messageList -> messageList.forEach(
                            message -> log.info("Analytics receive 'created-order' {}", message.getPayload())
                    )
            );
            ack.acknowledge();
        } catch (Exception e) {
            throw e;
        }
    }
}