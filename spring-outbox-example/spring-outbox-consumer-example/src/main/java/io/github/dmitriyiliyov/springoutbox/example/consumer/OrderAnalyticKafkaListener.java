package io.github.dmitriyiliyov.springoutbox.example.consumer;

import io.github.dmitriyiliyov.springoutbox.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.springoutbox.example.shared.OrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * It is obvious that in a real-world microservices architecture, different services would typically
 * subscribe to different topics based on their domain responsibilities.
 * This example consumer listens to all order-related topics for demonstration purposes only,
 * simulating an analytics service that needs to track all order lifecycle events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAnalyticKafkaListener {

    private final OutboxIdempotentConsumer outboxConsumer;

    @KafkaListener(topics = {"orders.updated", "orders.deleted"}, groupId = "analytics")
    public void listen(ConsumerRecord<String, OrderDto> record) {
        outboxConsumer.consume(record, () -> log.info("Analytics receive action with order {}", record.value()));
    }

    @KafkaListener(topics = "orders.created", groupId = "analytics", containerFactory = "orderBatchFactory")
    public void listenBatch(List<ConsumerRecord<String, OrderDto>> records) {
        outboxConsumer.consume(records, (recordList) -> recordList.forEach(record -> log.info("Analytics receive created order {}", record.value())));
    }
}