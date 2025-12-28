package io.github.dmitriyiliyov.springoutbox.consumer;

import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.UUID;

public class SpringMessageOutboxEventIdResolver implements OutboxEventIdResolver<Message<?>>{

    @Override
    public UUID resolve(Message<?> rowMessage) {
        MessageHeaders headers = rowMessage.getHeaders();
        String headerName = OutboxHeaders.EVENT_ID.getValue();
        UUID eventId = headers.get(headerName, UUID.class);
        if (eventId == null) {
            throw new IllegalArgumentException("Header '%s' not found, cannot resolve".formatted(headerName));
        }
        return eventId;
    }

    @Override
    public Class<?> getSupports() {
        return Message.class;
    }
}
