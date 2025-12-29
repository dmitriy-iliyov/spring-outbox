package io.github.dmitriyiliyov.springoutbox.unit.consumer.metrics;

import io.github.dmitriyiliyov.springoutbox.consumer.ConsumedOutboxManager;
import io.github.dmitriyiliyov.springoutbox.consumer.metrics.ConsumedOutboxManagerMetricsDecorator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumedOutboxManagerMetricsDecoratorUnitTests {

    @Mock
    private ConsumedOutboxManager delegate;

    @Mock
    private MeterRegistry registry;

    @Mock
    private Counter duplicatedCounter;

    @Mock
    private Counter consumedCounter;

    @Mock
    private Counter cleanedCounter;

    private ConsumedOutboxManagerMetricsDecorator decorator;

    @BeforeEach
    void setUp() {
        when(registry.counter(eq("consumed_outbox_events_total"), eq("type"), eq("duplicated")))
                .thenReturn(duplicatedCounter);
        when(registry.counter(eq("consumed_outbox_events_total"), eq("type"), eq("consumed")))
                .thenReturn(consumedCounter);
        when(registry.counter(eq("consumed_outbox_events_total"), eq("type"), eq("cleaned")))
                .thenReturn(cleanedCounter);

        decorator = new ConsumedOutboxManagerMetricsDecorator(delegate, registry);
    }

    @Test
    @DisplayName("UT isConsumed() when event is consumed should increment duplicated counter")
    void isConsumed_whenEventIsConsumed_shouldIncrementDuplicatedCounter() {
        // given
        UUID id = UUID.randomUUID();
        when(delegate.isConsumed(id)).thenReturn(true);

        // when
        boolean result = decorator.isConsumed(id);

        // then
        assertThat(result).isTrue();
        verify(delegate).isConsumed(id);
        verify(duplicatedCounter).increment();
        verify(consumedCounter, never()).increment();
    }

    @Test
    @DisplayName("UT isConsumed() when event is not consumed should increment consumed counter")
    void isConsumed_whenEventIsNotConsumed_shouldIncrementConsumedCounter() {
        // given
        UUID id = UUID.randomUUID();
        when(delegate.isConsumed(id)).thenReturn(false);

        // when
        boolean result = decorator.isConsumed(id);

        // then
        assertThat(result).isFalse();
        verify(delegate).isConsumed(id);
        verify(consumedCounter).increment();
        verify(duplicatedCounter, never()).increment();
    }

    @Test
    @DisplayName("UT filterConsumed() when no duplicates should increment consumed counter with total count")
    void filterConsumed_whenNoDuplicates_shouldIncrementConsumedCounterWithTotalCount() {
        // given
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        Set<UUID> alreadyConsumed = Set.of();
        when(delegate.filterConsumed(ids)).thenReturn(alreadyConsumed);

        // when
        Set<UUID> result = decorator.filterConsumed(ids);

        // then
        assertThat(result).isEmpty();
        verify(delegate).filterConsumed(ids);
        verify(duplicatedCounter).increment(0.0);
        verify(consumedCounter).increment(3.0);
    }

    @Test
    @DisplayName("UT filterConsumed() when all duplicates should increment duplicated counter with total count")
    void filterConsumed_whenAllDuplicates_shouldIncrementDuplicatedCounterWithTotalCount() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2);
        Set<UUID> alreadyConsumed = Set.of(id1, id2);
        when(delegate.filterConsumed(ids)).thenReturn(alreadyConsumed);

        // when
        Set<UUID> result = decorator.filterConsumed(ids);

        // then
        assertThat(result).hasSize(2);
        verify(delegate).filterConsumed(ids);
        verify(duplicatedCounter).increment(2.0);
        verify(consumedCounter).increment(0.0);
    }

    @Test
    @DisplayName("UT filterConsumed() when partial duplicates should increment both counters correctly")
    void filterConsumed_whenPartialDuplicates_shouldIncrementBothCountersCorrectly() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2, id3);
        Set<UUID> alreadyConsumed = Set.of(id1);
        when(delegate.filterConsumed(ids)).thenReturn(alreadyConsumed);

        // when
        Set<UUID> result = decorator.filterConsumed(ids);

        // then
        assertThat(result).hasSize(1);
        verify(delegate).filterConsumed(ids);
        verify(duplicatedCounter).increment(1.0);
        verify(consumedCounter).increment(2.0);
    }

    @Test
    @DisplayName("UT filterConsumed() when empty set should not increment any counter")
    void filterConsumed_whenEmptySet_shouldNotIncrementAnyCounter() {
        // given
        Set<UUID> ids = Set.of();
        Set<UUID> alreadyConsumed = Set.of();
        when(delegate.filterConsumed(ids)).thenReturn(alreadyConsumed);

        // when
        Set<UUID> result = decorator.filterConsumed(ids);

        // then
        assertThat(result).isEmpty();
        verify(delegate).filterConsumed(ids);
        verify(duplicatedCounter).increment(0.0);
        verify(consumedCounter).increment(0.0);
    }

    @Test
    @DisplayName("UT cleanBatchByTtl() when events cleaned should increment cleaned counter")
    void cleanBatchByTtl_whenEventsCleaned_shouldIncrementCleanedCounter() {
        // given
        Duration ttl = Duration.ofHours(1);
        int batchSize = 100;
        int cleanedCount = 50;
        when(delegate.cleanBatchByTtl(ttl, batchSize)).thenReturn(cleanedCount);

        // when
        int result = decorator.cleanBatchByTtl(ttl, batchSize);

        // then
        assertThat(result).isEqualTo(50);
        verify(delegate).cleanBatchByTtl(ttl, batchSize);
        verify(cleanedCounter).increment(50.0);
    }

    @Test
    @DisplayName("UT cleanBatchByTtl() when no events cleaned should increment cleaned counter with zero")
    void cleanBatchByTtl_whenNoEventsCleaned_shouldIncrementCleanedCounterWithZero() {
        // given
        Duration ttl = Duration.ofHours(1);
        int batchSize = 100;
        when(delegate.cleanBatchByTtl(ttl, batchSize)).thenReturn(0);

        // when
        int result = decorator.cleanBatchByTtl(ttl, batchSize);

        // then
        assertThat(result).isZero();
        verify(delegate).cleanBatchByTtl(ttl, batchSize);
        verify(cleanedCounter).increment(0.0);
    }
}