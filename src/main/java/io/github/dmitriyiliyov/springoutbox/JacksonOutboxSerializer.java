package io.github.dmitriyiliyov.springoutbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.OutboxSerializer;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.utils.UuidGenerator;

import java.util.List;
import java.util.UUID;

public final class JacksonOutboxSerializer implements OutboxSerializer {

    private final ObjectMapper mapper;
    private final UuidGenerator uuidGenerator;

    public JacksonOutboxSerializer(ObjectMapper mapper, UuidGenerator uuidGenerator) {
        this.mapper = mapper;
        this.uuidGenerator = uuidGenerator;
    }

    @Override
    public <T> OutboxEvent serialize(String eventType, T event) {
        try {
            UUID id = uuidGenerator.generate();
            String payloadType = event.getClass().getSimpleName();
            String payload = mapper.writeValueAsString(event);
            return new OutboxEvent(id, eventType, payloadType,  payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public <T> List<OutboxEvent> serialize(String eventType, List<T> rowEvents) {
        return rowEvents.stream()
                .map(event -> serialize(eventType, event))
                .toList();
    }
}
