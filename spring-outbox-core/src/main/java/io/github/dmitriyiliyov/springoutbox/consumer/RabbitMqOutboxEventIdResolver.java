package io.github.dmitriyiliyov.springoutbox.consumer;

import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxConstants;
import org.springframework.amqp.core.Message;

import java.util.Map;
import java.util.UUID;

public class RabbitMqOutboxEventIdResolver implements OutboxEventIdResolver<Message>{

    @Override
    public UUID resolve(Message rowMessage) {
        Map<String, Object> headers = rowMessage.getMessageProperties().getHeaders();
        String headerName = OutboxConstants.EVENT_ID_HEADER.getValue();
        Object rowEventId = headers.get(headerName);
        if (rowEventId == null) {
            throw new IllegalArgumentException("Header '%s' not found; cannot resolve".formatted(headerName));
        }
        if (rowEventId instanceof UUID uuid) {
            return uuid;
        }
        if (rowEventId instanceof String stringUuid) {
            return UUID.fromString(stringUuid);
        }
        throw new IllegalArgumentException("Header '%s' has invalid type; cannot resolve".formatted(headerName));
    }

    @Override
    public boolean supports(Class<?> c) {
        return Message.class.isAssignableFrom(c);
    }
}
