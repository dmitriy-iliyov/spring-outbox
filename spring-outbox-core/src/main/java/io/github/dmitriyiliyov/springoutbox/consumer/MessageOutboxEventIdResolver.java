package io.github.dmitriyiliyov.springoutbox.consumer;

import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxConstants;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.UUID;

public class MessageOutboxEventIdResolver<T> implements OutboxEventIdResolver<Message<T>>{

    @Override
    public UUID resolve(Message<T> rowMessage) {
        MessageHeaders headers = rowMessage.getHeaders();
        String headerName = OutboxConstants.EVENT_ID_HEADER.getValue();
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
