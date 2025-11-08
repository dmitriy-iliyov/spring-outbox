package io.github.dmitriyiliyov.springoutbox.unit.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.publisher.core.KafkaOutboxSender;
import io.github.dmitriyiliyov.springoutbox.publisher.core.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.publisher.core.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.publisher.core.domain.SenderResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KafkaOutboxSenderUnitTests {

    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    ObjectMapper mapper;

    @InjectMocks
    KafkaOutboxSender tested;

    @Test
    @DisplayName("UT sendEvents(), when ids is null should return empty sender result")
    public void sendEvents_whenIdsIsNull_shouldNotSendEvents() {
        // given
        String topic = "test-topic";
        List<OutboxEvent> events = null;

        // when
        SenderResult result = tested.sendEvents(topic, events);

        // then
        assertNull(result.failedIds());
        assertNull(result.processedIds());
        verifyNoInteractions(kafkaTemplate, mapper);
    }

    @Test
    @DisplayName("UT sendEvents(), when ids is empty should send events")
    public void sendEvents_whenIdsIsEmpty_shouldNotSendEvents() {
        // given
        String topic = "test-topic";
        List<OutboxEvent> events = List.of();

        // when
        SenderResult result = tested.sendEvents(topic, events);

        // then
        assertNull(result.failedIds());
        assertNull(result.processedIds());
        verifyNoInteractions(kafkaTemplate, mapper);
    }

    @Test
    @DisplayName("UT sendEvents(), should send events")
    public void sendEvents_shouldSendEvents() throws JsonProcessingException {
        // given
        String topic = "test-topic";
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                EventStatus.PENDING,
                "TestOutboxEvent",
                TestOutboxEvent.class.getName(),
                "{\"id\":\"%s\"}".formatted(UUID.randomUUID()),
                0,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
        List<OutboxEvent> events = List.of(event);
        when(kafkaTemplate.send(eq(topic), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(mapper.readValue(anyString(), any(Class.class))).thenReturn(new Object());

        // when
        SenderResult result = tested.sendEvents(topic, events);

        // then
        assertEquals(Set.of(event.getId()), result.processedIds());
        assertEquals(Set.of(), result.failedIds());
        verify(kafkaTemplate, times(events.size())).send(any(Message.class));
        verify(mapper, times(events.size())).readValue(anyString(), any(Class.class));
        verifyNoMoreInteractions(kafkaTemplate, mapper);
    }

    @Test
    @DisplayName("UT sendEvents() when Kafka throws, should return correct SenderResult")
    public void sendEvents_whenKafkaThrows_shouldReturnSenderResult() throws JsonProcessingException {
        // given
        String topic = "test-topic";
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                EventStatus.PENDING,
                "TestOutboxEvent",
                TestOutboxEvent.class.getName(),
                "{\"id\":\"%s\"}".formatted(UUID.randomUUID()),
                0,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
        List<OutboxEvent> events = List.of(event);
        when(kafkaTemplate.send(eq(topic), any())).thenThrow(new RuntimeException());
        when(mapper.readValue(anyString(), any(Class.class))).thenReturn(new Object());

        // when
        SenderResult result = tested.sendEvents(topic, events);

        // then
        assertEquals(Set.of(event.getId()), result.failedIds());
        assertEquals(Set.of(), result.processedIds());
        verify(kafkaTemplate, times(events.size())).send(any(Message.class));
        verify(mapper, times(events.size())).readValue(anyString(), any(Class.class));
        verifyNoMoreInteractions(kafkaTemplate, mapper);
    }

    @Test
    @DisplayName("UT sendEvents() when Kafka throws, should return correct SenderResult")
    public void sendEvents_whenCompletableFutureThrows_shouldReturnSenderResult() throws JsonProcessingException {
        // given
        String topic = "test-topic";
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                EventStatus.PENDING,
                "TestOutboxEvent",
                TestOutboxEvent.class.getName(),
                "{\"id\":\"%s\"}".formatted(UUID.randomUUID()),
                0,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
        List<OutboxEvent> events = List.of(event);
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka send failed"));
        when(kafkaTemplate.send(eq(topic), any())).thenReturn(future);
        when(mapper.readValue(anyString(), any(Class.class))).thenReturn(new Object());

        // when
        SenderResult result = tested.sendEvents(topic, events);

        // then
        assertEquals(Set.of(event.getId()), result.failedIds());
        assertEquals(Set.of(), result.processedIds());
        verify(kafkaTemplate, times(events.size())).send(any(Message.class));
        verify(mapper, times(events.size())).readValue(anyString(), any(Class.class));
        verifyNoMoreInteractions(kafkaTemplate, mapper);
    }
}
