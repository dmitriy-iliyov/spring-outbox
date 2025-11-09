package io.github.dmitriyiliyov.springoutbox.example;

import io.github.dmitriyiliyov.springoutbox.consumer.OutboxEventIdResolver;
import io.github.dmitriyiliyov.springoutbox.consumer.OutboxEventIdResolverManager;
import io.github.dmitriyiliyov.springoutbox.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.springoutbox.example.shared.OrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;


/**
 * It is obvious that in a real-world microservices architecture, different services would typically
 * subscribe to different topics based on their domain responsibilities.
 * This example consumer listens to all order-related topics for demonstration purposes only,
 * simulating an analytics service that needs to track all order lifecycle events.
 */
@Slf4j
@RequiredArgsConstructor
public class OrderAnalyticKafkaListener {

    private final OutboxEventIdResolver<Object> eventIdResolver;
    private final OutboxIdempotentConsumer outboxConsumer;

    @KafkaListener(topics = {"orders.created", "orders.updated", "orders.delete"}, groupId = "analytics")
    public void listenCreateOrder(ConsumerRecord<String, OrderDto> record) {
        outboxConsumer.consume(
                eventIdResolver.resolve(record),
                () -> log.info("Analytics receive {}", record.value())
        );
    }
}