package io.github.dmitriyiliyov.oncebox.example.consumer;

import io.github.dmitriyiliyov.oncebox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.example.shared.OrderDto;
import io.github.dmitriyiliyov.oncebox.messaging.OutboxHeadersUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.List;

@Profile("rabbit")
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderAnalyticRabbitListener {

    private final OutboxIdempotentConsumer outboxConsumer;

    @RabbitListener(queues = "orders", containerFactory = "outboxRabbitListenerContainerFactory")
    public void listen(Message<?> message) {
        outboxConsumer.consume(
                message,
                OutboxHeadersUtils::extractId,
                m -> log.info("Analytics receive '{}' event {}", OutboxHeadersUtils.extractEventType(m), m.getPayload())
        );
    }

    @RabbitListener(queues = "orders.created", containerFactory = "rabbitBatchListenerContainerFactory")
    public void listenBatch(List<Message<OrderDto>> messages) {
        outboxConsumer.consume(
                messages,
                OutboxHeadersUtils::extractId,
                m -> m.forEach(
                        message -> log.info("Analytics receive '{}' event {}", OutboxHeadersUtils.extractEventType(message), message.getPayload())
                )
        );
    }
}
