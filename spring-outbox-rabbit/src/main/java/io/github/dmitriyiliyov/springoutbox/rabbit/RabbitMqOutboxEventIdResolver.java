package io.github.dmitriyiliyov.springoutbox.rabbit;

import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxEventIdResolver;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxHeaders;
import org.springframework.amqp.core.Message;

import java.util.Map;
import java.util.UUID;

public class RabbitMqOutboxEventIdResolver implements OutboxEventIdResolver<Message> {

    @Override
    public UUID resolve(Message rawMessage) {
        Map<String, Object> headers = rawMessage.getMessageProperties().getHeaders();
        String headerName = OutboxHeaders.EVENT_ID.getValue();
        Object rawEventId = headers.get(headerName);
        if (rawEventId == null) {
            throw new IllegalArgumentException("Header '%s' not found; cannot resolve".formatted(headerName));
        }
        if (rawEventId instanceof UUID uuid) {
            return uuid;
        }
        if (rawEventId instanceof String stringUuid) {
            return UUID.fromString(stringUuid);
        }
        throw new IllegalArgumentException(
                "Header '%s' has invalid type %s; cannot resolve".formatted(headerName, rawEventId.getClass().getName())
        );
    }

    @Override
    public Class<?> getSupports() {
        return Message.class;
    }
}
