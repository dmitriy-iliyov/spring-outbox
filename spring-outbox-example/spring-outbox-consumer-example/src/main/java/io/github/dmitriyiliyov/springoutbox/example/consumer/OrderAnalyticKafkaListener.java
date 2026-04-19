package io.github.dmitriyiliyov.springoutbox.example.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxHeaders;
import io.github.dmitriyiliyov.springoutbox.example.shared.OrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


@Profile("kafka")
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderAnalyticKafkaListener {

    private final OutboxIdempotentConsumer outboxConsumer;
    private final Map<String, Consumer<ConsumerRecord<String, OrderDto>>> handlers = Map.of(
            "update-order", (record) -> log.info("Analytics receive 'update-order' event {}", record.value()),
            "delete-order", (record) -> log.info("Analytics receive 'delete-order' event {}", record.value())
    );

    /**
     * Example of routing messages to different handlers based on the event type.
     * The event type is extracted from the Kafka record headers using OutboxHeaders.EVENT_TYPE.
     * This demonstrates how Outbox pattern headers can be used to dynamically dispatch messages.
     */
    @KafkaListener(topics = "orders", groupId = "analytics")
    public void listen(ConsumerRecord<String, OrderDto> record, Acknowledgment ack) {
        try {
            String eventType = new String(
                    record.headers()
                            .lastHeader(OutboxHeaders.EVENT_TYPE.getValue())
                            .value()
            );
            outboxConsumer.consume(record, () -> {
                Consumer<ConsumerRecord<String, OrderDto>> handler = handlers.get(eventType);
                if (handler != null) {
                    handler.accept(record);
                }
            });
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
    @KafkaListener(topics = "orders.created", groupId = "analytics", containerFactory = "orderBatchFactory")
    public void listenBatch(List<ConsumerRecord<String, OrderDto>> records, Acknowledgment ack) {
        try {
            outboxConsumer.consume(
                    records,
                    (recordList) -> recordList.forEach(
                            record -> log.info("Analytics receive 'created-order' {}", record.value())
                    )
            );
            ack.acknowledge();
        } catch (Exception e) {
            throw e;
        }
    }
}