package io.github.dmitriyiliyov.springoutbox.unit.core;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;
import io.github.dmitriyiliyov.springoutbox.core.DefaultOutboxProcessor;
import io.github.dmitriyiliyov.springoutbox.core.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.OutboxSender;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.domain.SenderResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultOutboxProcessorUnitTests {

    @Mock
    OutboxManager manager;

    @Mock
    OutboxSender sender;

    @InjectMocks
    DefaultOutboxProcessor tested;

    OutboxProperties.EventProperties properties;
    String eventType;
    String topic;
    int batchSize;
    int maxRetries;

    @BeforeEach
    void setUpProperties() {
        properties = mock(OutboxProperties.EventProperties.class);
        eventType = "test-event-type";
        topic = "test-topic";
        batchSize = 10;
        maxRetries = 1;

        lenient().when(properties.eventType()).thenReturn("test-event-type");
        lenient().when(properties.topic()).thenReturn("test-topic");
        lenient().when(properties.batchSize()).thenReturn(10);
        lenient().when(properties.maxRetries()).thenReturn(1);
    }

    @Test
    @DisplayName("UT process() when properties non null, should send and finalize")
    void process_whenPropertiesNonNull_shouldSendAndFinalize() {
        // given
        OutboxEvent event1 = mock(OutboxEvent.class);
        OutboxEvent event2 = mock(OutboxEvent.class);
        List<OutboxEvent> events = List.of(event1, event2);
        UUID processedId = UUID.randomUUID();
        UUID failedId = UUID.randomUUID();
        Set<UUID> processedIds = Set.of(processedId);
        Set<UUID> failedIds = Set.of(failedId);
        SenderResult senderResult = new SenderResult(processedIds, failedIds);

        when(manager.loadBatch(eventType, batchSize)).thenReturn(events);
        when(sender.sendEvents(topic, events)).thenReturn(senderResult);

        // when
        tested.process(properties);

        // then
        verify(manager, times(1)).loadBatch(eventType, batchSize);
        verify(sender, times(1)).sendEvents(topic, events);
        verify(manager, times(1)).finalizeBatch(eq(events), eq(processedIds), eq(failedIds),
                eq(maxRetries), any(Function.class));
        verifyNoMoreInteractions(manager, sender);
    }

    @Test
    @DisplayName("UT process() when sender throws, should finalize all as failed")
    void process_whenSenderThrows_shouldFinalizeAllAsFailed() {
        // given
        OutboxEvent event1 = mock(OutboxEvent.class);
        OutboxEvent event2 = mock(OutboxEvent.class);
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(event1.getId()).thenReturn(id1);
        when(event2.getId()).thenReturn(id2);
        List<OutboxEvent> events = List.of(event1, event2);

        when(manager.loadBatch(eventType, batchSize)).thenReturn(events);
        when(sender.sendEvents(topic, events)).thenThrow(RuntimeException.class);

        // when
        tested.process(properties);

        // then
        ArgumentCaptor<Set<UUID>> processedCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Set<UUID>> failedCaptor = ArgumentCaptor.forClass(Set.class);

        verify(manager).loadBatch(eventType, batchSize);
        verify(sender).sendEvents(topic, events);
        verify(manager).finalizeBatch(eq(events), processedCaptor.capture(), failedCaptor.capture(), eq(maxRetries), any(Function.class));

        assertThat(processedCaptor.getValue()).isNull();
        assertThat(failedCaptor.getValue()).containsExactlyInAnyOrder(id1, id2);
        verifyNoMoreInteractions(manager, sender);
    }

    @Test
    @DisplayName("UT process() when properties is null, should throw NPE")
    void process_whenPropertiesAreNull_shouldThrow() {
        assertThrows(NullPointerException.class, () -> tested.process(null));
        verifyNoInteractions(manager, sender);
    }

    @Test @DisplayName("UT process() when loaded events is null, should early returns") public void process_whenLoadedEventsIsNull_shouldEarlyReturns() {
        // given
        when(manager.loadBatch(eventType, batchSize)).thenReturn(null);

        // when
        tested.process(properties);

        // then
        verify(manager, times(1)).loadBatch(eventType, batchSize);
        verifyNoMoreInteractions(manager);
    }

    @Test
    @DisplayName("UT process() when loaded events is empty, should early returns")
    void process_whenLoadedEventsIsEmpty_shouldEarlyReturn() {
        when(manager.loadBatch(eventType, batchSize)).thenReturn(List.of());

        tested.process(properties);

        verify(manager).loadBatch(eventType, batchSize);
        verifyNoMoreInteractions(manager, sender);
    }
}
