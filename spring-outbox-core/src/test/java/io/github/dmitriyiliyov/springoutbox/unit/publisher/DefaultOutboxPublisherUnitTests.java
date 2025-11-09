package io.github.dmitriyiliyov.springoutbox.unit.publisher;

import io.github.dmitriyiliyov.springoutbox.publisher.DefaultOutboxPublisher;
import io.github.dmitriyiliyov.springoutbox.publisher.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.publisher.OutboxSerializer;
import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherProperties;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultOutboxPublisherUnitTests {

    @Mock
    OutboxPublisherProperties properties;

    @Mock
    OutboxSerializer serializer;

    @Mock
    OutboxManager manager;

    @InjectMocks
    DefaultOutboxPublisher tested;

    String eventType;
    Object event;
    OutboxEvent serializedEvent;

    @BeforeEach
    void setUp() {
        eventType = "user-created";
        event = new Object();
        serializedEvent = mock(OutboxEvent.class);

        lenient().when(properties.existEventType(eventType)).thenReturn(true);
    }

    @Test
    @DisplayName("UT publish(single) should serialize and save event")
    void publish_single_shouldSerializeAndSave() {
        // given
        when(serializer.serialize(eventType, event)).thenReturn(serializedEvent);

        // when
        tested.publish(eventType, event);

        // then
        verify(properties).existEventType(eventType);
        verify(serializer).serialize(eventType, event);
        verify(manager).save(serializedEvent);
        verifyNoMoreInteractions(properties, serializer, manager);
    }

    @Test
    @DisplayName("UT publish(single) when eventType blank should throw IAE")
    void publish_single_whenEventTypeBlank_shouldThrow() {
        // when
        assertThrows(IllegalArgumentException.class, () -> tested.publish("  ", event));

        // then
        verifyNoInteractions(manager, serializer);
    }

    @Test
    @DisplayName("UT publish(single) when eventType non existing should throw IAE")
    void publish_single_whenEventTypeNonExisting_shouldThrow() {
        // when
        when(properties.existEventType(eventType)).thenReturn(false);

        // then
        assertThrows(IllegalArgumentException.class, () -> tested.publish(eventType, event));
        verify(properties).existEventType(eventType);
        verifyNoInteractions(manager, serializer);
    }

    @Test
    @DisplayName("UT publish(single) when event is null should throw NPE")
    void publish_single_whenEventNull_shouldThrow() {
        // when
        assertThrows(NullPointerException.class, () -> tested.publish(eventType, null));

        // then
        verify(properties).existEventType(eventType);
        verifyNoInteractions(manager, serializer);
    }

    @Test
    @DisplayName("UT publish(batch) should serialize and save batch")
    void publish_batch_shouldSerializeAndSaveBatch() {
        // given
        List<Object> events = List.of(event, new Object());
        List<OutboxEvent> serializedBatch = List.of(serializedEvent, mock(OutboxEvent.class));

        when(serializer.serialize(eventType, events)).thenReturn(serializedBatch);

        // when
        tested.publish(eventType, events);

        // then
        verify(properties).existEventType(eventType);
        verify(serializer).serialize(eventType, events);
        verify(manager).saveBatch(serializedBatch);
        verifyNoMoreInteractions(properties, serializer, manager);
    }

    @Test
    @DisplayName("UT publish(batch) when events null should throw NPE")
    void publish_batch_whenNull_shouldThrow() {
        // when
        assertThrows(NullPointerException.class, () -> tested.publish(eventType, null));

        // then
        verify(properties).existEventType(eventType);
        verifyNoInteractions(manager, serializer);
    }

    @Test
    @DisplayName("UT publish(batch) when empty should log warning and not save")
    void publish_batch_whenEmpty_shouldWarnAndNotSave() {
        // when
        tested.publish(eventType, List.of());

        // then
        verify(properties).existEventType(eventType);
        verifyNoInteractions(manager, serializer);
    }

    @Test
    @DisplayName("UT publish(batch) when non existing eventType should throw IAE")
    void publish_batch_whenNonExistingType_shouldThrow() {
        // when
        when(properties.existEventType(eventType)).thenReturn(false);

        // then
        assertThrows(IllegalArgumentException.class, () -> tested.publish(eventType, List.of(event)));
        verify(properties).existEventType(eventType);
        verifyNoInteractions(manager, serializer);
    }

    @Test
    @DisplayName("UT publish(batch) when eventType blank should throw IAE")
    void publish_batch_whenBlankEventType_shouldThrow() {
        // when
        assertThrows(IllegalArgumentException.class, () -> tested.publish(" ", List.of(event)));

        // then
        verifyNoInteractions(manager, serializer);
    }
}
