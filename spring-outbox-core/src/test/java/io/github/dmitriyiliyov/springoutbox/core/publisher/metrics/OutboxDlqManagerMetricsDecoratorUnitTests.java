package io.github.dmitriyiliyov.springoutbox.core.publisher.metrics;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxDlqManagerMetricsDecoratorUnitTests {

    @Mock
    OutboxDlqManager delegate;

    @Mock
    OutboxPublisherPropertiesHolder properties;

    SimpleMeterRegistry registry;
    OutboxDlqManagerMetricsDecorator tested;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        Map<String, OutboxPublisherPropertiesHolder.EventPropertiesHolder> eventProps = Map.of(
                "test-event", mock(OutboxPublisherPropertiesHolder.EventPropertiesHolder.class)
        );
        when(properties.getEventHolders()).thenReturn(eventProps);
        tested = new OutboxDlqManagerMetricsDecorator(delegate, properties, registry);
    }

    @Test
    @DisplayName("UT saveBatch() when events null should delegate and not increment counters")
    void saveBatch_whenEventsNull_shouldDelegateAndNotIncrement() {
        // when
        tested.saveBatch(null);

        // then
        verify(delegate).saveBatch(null);
        assertEquals(
                0,
                registry.getMeters().stream()
                        .filter(m -> m instanceof Counter)
                        .mapToDouble(m -> ((Counter) m).count())
                        .sum()
        );
    }

    @Test
    @DisplayName("UT saveBatch() when events empty should delegate and not increment counters")
    void saveBatch_whenEventsEmpty_shouldDelegateAndNotIncrement() {
        // when
        tested.saveBatch(List.of());

        // then
        verify(delegate).saveBatch(List.of());
        assertEquals(
                0,
                registry.getMeters().stream()
                        .filter(m -> m instanceof Counter)
                        .mapToDouble(m -> ((Counter) m).count())
                        .sum()
        );
    }

    @Test
    @DisplayName("UT saveBatch() when events present should delegate and increment counters")
    void saveBatch_whenEventsPresent_shouldDelegateAndIncrement() {
        // given
        OutboxDlqEvent event1 = mock(OutboxDlqEvent.class);
        when(event1.getEventType()).thenReturn("test-event");
        when(event1.getDlqStatus()).thenReturn(DlqStatus.MOVED);

        OutboxDlqEvent event2 = mock(OutboxDlqEvent.class);
        when(event2.getEventType()).thenReturn("test-event");
        when(event2.getDlqStatus()).thenReturn(DlqStatus.RESOLVED);

        List<OutboxDlqEvent> events = List.of(event1, event2);

        // when
        tested.saveBatch(events);

        // then
        verify(delegate).saveBatch(events);
        Counter movedCounter = registry.get("outbox_dlq_events_rate_total")
                .tag("event_type", "test-event")
                .tag("status", "moved")
                .counter();
        assertEquals(1.0, movedCounter.count());

        Counter resolvedCounter = registry.get("outbox_dlq_events_rate_total")
                .tag("event_type", "test-event")
                .tag("status", "resolved")
                .counter();
        assertEquals(1.0, resolvedCounter.count());
    }

    @Test
    @DisplayName("UT saveBatch() when event type unknown should delegate and not increment counters")
    void saveBatch_whenEventTypeUnknown_shouldDelegateAndNotIncrement() {
        // given
        OutboxDlqEvent event = mock(OutboxDlqEvent.class);
        when(event.getEventType()).thenReturn("unknown-event");
        List<OutboxDlqEvent> events = List.of(event);

        // when
        tested.saveBatch(events);

        // then
        verify(delegate).saveBatch(events);
        assertEquals(
                0,
                registry.getMeters().stream()
                        .filter(m -> m instanceof Counter)
                        .mapToDouble(m -> ((Counter) m).count())
                        .sum()
        );
    }

    @Test
    @DisplayName("UT loadAndLockBatch() when events returned should increment attempt counter")
    void loadAndLockBatch_whenEventsReturned_shouldIncrementAttemptCounter() {
        // given
        List<OutboxDlqEvent> events = List.of(mock(OutboxDlqEvent.class), mock(OutboxDlqEvent.class));
        when(delegate.loadAndLockBatch(DlqStatus.MOVED, 10)).thenReturn(events);

        // when
        tested.loadAndLockBatch(DlqStatus.MOVED, 10);

        // then
        verify(delegate).loadAndLockBatch(DlqStatus.MOVED, 10);
        Counter attemptCounter = registry.get("outbox_dlq_events_by_type_rate_total")
                .tag("type", "attempt_move_to_outbox")
                .counter();
        assertEquals(2.0, attemptCounter.count());
    }

    @Test
    @DisplayName("UT loadAndLockBatch() when null returned should not increment attempt counter")
    void loadAndLockBatch_whenNullReturned_shouldNotIncrement() {
        // given
        when(delegate.loadAndLockBatch(DlqStatus.MOVED, 10)).thenReturn(null);

        // when
        tested.loadAndLockBatch(DlqStatus.MOVED, 10);

        // then
        verify(delegate).loadAndLockBatch(DlqStatus.MOVED, 10);
        Counter attemptCounter = registry.get("outbox_dlq_events_by_type_rate_total")
                .tag("type", "attempt_move_to_outbox")
                .counter();
        assertEquals(0.0, attemptCounter.count());
    }

    @Test
    @DisplayName("UT loadAndLockBatch() when empty returned should not increment attempt counter")
    void loadAndLockBatch_whenEmptyReturned_shouldNotIncrement() {
        // given
        when(delegate.loadAndLockBatch(DlqStatus.MOVED, 10)).thenReturn(List.of());

        // when
        tested.loadAndLockBatch(DlqStatus.MOVED, 10);

        // then
        verify(delegate).loadAndLockBatch(DlqStatus.MOVED, 10);
        Counter attemptCounter = registry.get("outbox_dlq_events_by_type_rate_total")
                .tag("type", "attempt_move_to_outbox")
                .counter();
        assertEquals(0.0, attemptCounter.count());
    }

    @Test
    @DisplayName("UT deleteById() should increment manual deleted counter")
    void deleteById_shouldIncrementManualDeletedCounter() {
        // given
        UUID id = UUID.randomUUID();
        when(delegate.deleteById(id)).thenReturn(1);

        // when
        tested.deleteById(id);

        // then
        verify(delegate).deleteById(id);
        Counter manualDeletedCounter = registry.get("outbox_dlq_events_by_type_rate_total")
                .tag("type", "manual_deleted")
                .counter();
        assertEquals(1.0, manualDeletedCounter.count());
    }

    @Test
    @DisplayName("UT deleteBatch() should increment success moved counter")
    void deleteBatch_shouldIncrementSuccessMovedCounter() {
        // given
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());
        when(delegate.deleteBatch(ids)).thenReturn(2);

        // when
        tested.deleteBatch(ids);

        // then
        verify(delegate).deleteBatch(ids);
        Counter successMovedCounter = registry.get("outbox_dlq_events_by_type_rate_total")
                .tag("type", "success_moved_to_outbox")
                .counter();
        assertEquals(2.0, successMovedCounter.count());
    }

    @Test
    @DisplayName("UT deleteBatchWithCheck() should increment manual deleted counter")
    void deleteBatchWithCheck_shouldIncrementManualDeletedCounter() {
        // given
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(delegate.deleteBatchWithCheck(ids)).thenReturn(3);

        // when
        tested.deleteBatchWithCheck(ids);

        // then
        verify(delegate).deleteBatchWithCheck(ids);
        Counter manualDeletedCounter = registry.get("outbox_dlq_events_by_type_rate_total")
                .tag("type", "manual_deleted")
                .counter();
        assertEquals(3.0, manualDeletedCounter.count());
    }
}
