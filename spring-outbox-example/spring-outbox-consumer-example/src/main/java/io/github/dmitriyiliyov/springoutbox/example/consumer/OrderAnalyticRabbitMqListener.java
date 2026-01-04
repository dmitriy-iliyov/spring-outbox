package io.github.dmitriyiliyov.springoutbox.example.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.springoutbox.example.shared.OrderDto;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

@Profile("rabbitmq")
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderAnalyticRabbitMqListener {

    private final OutboxIdempotentConsumer outboxConsumer;
    private final ObjectMapper mapper;

    private final Map<String, Consumer<Message>> handlers = Map.of(
            "create-order", (message) -> log.info("Analytics receive 'create-order' event {}", converterToOrderDto(message)),
            "update-order", (message) -> log.info("Analytics receive 'update-order' event {}", converterToOrderDto(message)),
            "delete-order", (message) -> log.info("Analytics receive 'delete-order' event {}", converterToLong(message))
    );

    @RabbitListener(queues = "orders")
    public void listenOrdersQueue(Message message) {
        String eventType = (String) message.getMessageProperties().getHeaders().get(OutboxHeaders.EVENT_TYPE.getValue());
        outboxConsumer.consume(message, () -> {
            Consumer<Message> handler = handlers.get(eventType);
            if (handler != null) {
                handler.accept(message);
            } else {
                log.warn("No handler found for event type '{}'", eventType);
            }
        });
    }

    @RabbitListener(queues = "orders.created")
    public void listenOrdersCreatedQueue(Message message) {
        outboxConsumer.consume(message, () -> {
            handlers.get("create-order").accept(message);
        });
    }

    private OrderDto converterToOrderDto(Message message) {
        try {
            return mapper.readValue(new String(message.getBody()), OrderDto.class);
        } catch (JsonProcessingException e) {
            log.error("Error when converting from json", e);
            throw new RuntimeException(e);
        }
    }

    private Long converterToLong(Message message) {
        try {
            return mapper.readValue(new String(message.getBody()), Long.class);
        } catch (JsonProcessingException e) {
            log.error("Error when converting from json", e);
            throw new RuntimeException(e);
        }
    }
}
