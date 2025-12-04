package io.github.dmitriyiliyov.springoutbox.unit.publisher;

import io.github.dmitriyiliyov.springoutbox.publisher.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherProperties;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.publisher.metrics.OutboxManagerMetricsDecorator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class OutboxManagerMetricsDecoratorUnitTests {

    private OutboxManager outboxManager;
    private OutboxPublisherProperties properties;
    private OutboxManagerMetricsDecorator tested;
    private SimpleMeterRegistry registry;
    private Counter processedCounter;
    private Counter failedCounter;

    @BeforeEach
    public void initContext() {
        outboxManager = mock(OutboxManager.class);
        registry = new SimpleMeterRegistry();
        Map<String, OutboxPublisherProperties.EventProperties> eventProps = Map.of(
                "test-event-type", new OutboxPublisherProperties.EventProperties(),
                "test-event-type-2", new OutboxPublisherProperties.EventProperties()
        );
        properties = mock(OutboxPublisherProperties.class);
        when(properties.getEvents()).thenReturn(eventProps);
        tested = new OutboxManagerMetricsDecorator(outboxManager, properties, registry);
        processedCounter = registry.get("outbox_events_rate_total")
                .tag("event_type", "test-event-type")
                .tag("status", "processed")
                .counter();
        failedCounter = registry.get("outbox_events_rate_total")
                .tag("event_type", "test-event-type")
                .tag("status", "failed")
                .counter();
    }

    @Test
    @DisplayName("UT finalizeBatch(): when event isn't empty and eventType != null, should delegate and involve counters")
    public void finalizeBatch_whenArgumentsValid_shouldDelegateAndGetCount() {
        // given
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), "test-event-type", "payloadType", "{}");
        List<OutboxEvent> events = List.of(event);
        Set<UUID> processedIds = Set.of(UUID.randomUUID(), UUID.randomUUID());
        Set<UUID> failedIds = Set.of(UUID.randomUUID());

        // when
        tested.finalizeBatch(events, processedIds, failedIds, 5, retry -> Instant.now());

        // then
        verify(outboxManager).finalizeBatch(eq(events), eq(processedIds), eq(failedIds), eq(5), any());
        assertEquals(2.0, processedCounter.count(), "Processed counter should be incremented by 2");
        assertEquals(1.0, failedCounter.count(), "Failed counter should be incremented by 1");
    }

    @Test
    @DisplayName("UT finalizeBatch(): when event isn empty, should delegate and not involve counters")
    public void finalizeBatch_whenEventsIsEmpty_shouldDelegateAndGetCount() {
        // given
        List<OutboxEvent> events = List.of();
        Set<UUID> processedIds = Set.of(UUID.randomUUID(), UUID.randomUUID());
        Set<UUID> failedIds = Set.of(UUID.randomUUID());

        // when
        tested.finalizeBatch(events, processedIds, failedIds, 5, retry -> Instant.now());

        // then
        verify(outboxManager).finalizeBatch(eq(events), eq(processedIds), eq(failedIds), eq(5), any());
        assertEquals(0, processedCounter.count(), "Processed counter should not be incremented");
        assertEquals(0, failedCounter.count(), "Failed counter should be not incremented");
    }

    @Test
    @DisplayName("UT finalizeBatch(): when event eventType == null, should delegate and not involve counters")
    public void finalizeBatch_whenEventTypeIsNull_shouldDelegateAndGetCount() {
        // given
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), null, "payloadType", "{}");
        List<OutboxEvent> events = List.of(event);
        Set<UUID> processedIds = Set.of(UUID.randomUUID(), UUID.randomUUID());
        Set<UUID> failedIds = Set.of(UUID.randomUUID());

        // when
        tested.finalizeBatch(events, processedIds, failedIds, 5, retry -> Instant.now());

        // then
        verify(outboxManager).finalizeBatch(eq(events), eq(processedIds), eq(failedIds), eq(5), any());
        assertEquals(0, processedCounter.count(), "Processed counter should not be incremented");
        assertEquals(0, failedCounter.count(), "Failed counter should be not incremented");
    }
}