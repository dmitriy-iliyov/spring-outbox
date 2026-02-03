package io.github.dmitriyiliyov.springoutbox.core.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.publisher.utils.UuidGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JacksonOutboxSerializerUnitTests {
    @Mock
    ObjectMapper mapper;

    @Mock
    UuidGenerator uuidGenerator;

    JacksonOutboxSerializer serializer;

    @BeforeEach
    void setup() {
        serializer = new JacksonOutboxSerializer(mapper, uuidGenerator);
    }

    @Test
    @DisplayName("serialize() should return OutboxEvent when serialization succeeds")
    void serialize_whenSuccess_shouldReturnOutboxEvent() throws Exception {
        // given
        UUID id = UUID.randomUUID();
        TestEvent event = new TestEvent("test");
        when(uuidGenerator.generate()).thenReturn(id);
        when(mapper.writeValueAsString(event)).thenReturn("{\"value\":\"test\"}");

        // when
        OutboxEvent result = serializer.serialize("type", event);

        // then
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getEventType()).isEqualTo("type");
        assertThat(result.getPayload()).isEqualTo("{\"value\":\"test\"}");
        assertThat(result.getPayloadType()).isEqualTo(TestEvent.class.getName());
        verify(uuidGenerator, times(1)).generate();
        verify(mapper, times(1)).writeValueAsString(event);
    }

    @Test
    @DisplayName("serialize() should wrap JsonProcessingException into OutboxSerializationException")
    void serialize_whenJsonProcessingException_shouldWrapToRuntime() throws Exception {
        // given
        TestEvent event = new TestEvent("fail");
        when(uuidGenerator.generate()).thenReturn(UUID.randomUUID());
        when(mapper.writeValueAsString(event)).thenThrow(JsonProcessingException.class);

        // when + then
        assertThrows(OutboxSerializationException.class, () -> serializer.serialize("type", event));
    }

    @Test
    @DisplayName("serialize() should rethrow unexpected exceptions directly")
    void serialize_whenUnexpectedException_shouldRethrow() {
        // given
        TestEvent event = new TestEvent("error");
        when(uuidGenerator.generate()).thenThrow(new IllegalStateException("UUID error"));

        // when + then
        assertThrows(IllegalStateException.class, () -> serializer.serialize("type", event));
    }

    record TestEvent(String value) {}
}
