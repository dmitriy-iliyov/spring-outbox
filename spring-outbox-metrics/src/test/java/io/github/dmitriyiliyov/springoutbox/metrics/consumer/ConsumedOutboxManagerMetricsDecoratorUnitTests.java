package io.github.dmitriyiliyov.springoutbox.metrics.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

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
        Mockito.when(registry.counter(ArgumentMatchers.eq("consumed_outbox_events_total"), ArgumentMatchers.eq("type"), ArgumentMatchers.eq("duplicated")))
                .thenReturn(duplicatedCounter);
        Mockito.when(registry.counter(ArgumentMatchers.eq("consumed_outbox_events_total"), ArgumentMatchers.eq("type"), ArgumentMatchers.eq("consumed")))
                .thenReturn(consumedCounter);
        Mockito.when(registry.counter(ArgumentMatchers.eq("consumed_outbox_events_total"), ArgumentMatchers.eq("type"), ArgumentMatchers.eq("cleaned")))
                .thenReturn(cleanedCounter);

        decorator = new ConsumedOutboxManagerMetricsDecorator(registry, delegate);
    }

    @Test
    @DisplayName("UT isConsumed() when event is consumed should increment duplicated counter")
    void isConsumed_whenEventIsConsumed_shouldIncrementDuplicatedCounter() {
        // given
        UUID id = UUID.randomUUID();
        Mockito.when(delegate.isConsumed(id)).thenReturn(true);

        // when
        boolean result = decorator.isConsumed(id);

        // then
        Assertions.assertThat(result).isTrue();
        Mockito.verify(delegate).isConsumed(id);
        Mockito.verify(duplicatedCounter).increment();
        Mockito.verify(consumedCounter, Mockito.never()).increment();
    }

    @Test
    @DisplayName("UT isConsumed() when event is not consumed should increment consumed counter")
    void isConsumed_whenEventIsNotConsumed_shouldIncrementConsumedCounter() {
        // given
        UUID id = UUID.randomUUID();
        Mockito.when(delegate.isConsumed(id)).thenReturn(false);

        // when
        boolean result = decorator.isConsumed(id);

        // then
        Assertions.assertThat(result).isFalse();
        Mockito.verify(delegate).isConsumed(id);
        Mockito.verify(consumedCounter).increment();
        Mockito.verify(duplicatedCounter, Mockito.never()).increment();
    }

    @Test
    @DisplayName("UT filterConsumed() when no duplicates should increment consumed counter with total count")
    void filterConsumed_whenNoDuplicates_shouldIncrementConsumedCounterWithTotalCount() {
        // given
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        Set<UUID> alreadyConsumed = Set.of();
        Mockito.when(delegate.filterConsumed(ids)).thenReturn(alreadyConsumed);

        // when
        Set<UUID> result = decorator.filterConsumed(ids);

        // then
        Assertions.assertThat(result).isEmpty();
        Mockito.verify(delegate).filterConsumed(ids);
        Mockito.verify(duplicatedCounter).increment(0.0);
        Mockito.verify(consumedCounter).increment(3.0);
    }

    @Test
    @DisplayName("UT filterConsumed() when all duplicates should increment duplicated counter with total count")
    void filterConsumed_whenAllDuplicates_shouldIncrementDuplicatedCounterWithTotalCount() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2);
        Set<UUID> alreadyConsumed = Set.of(id1, id2);
        Mockito.when(delegate.filterConsumed(ids)).thenReturn(alreadyConsumed);

        // when
        Set<UUID> result = decorator.filterConsumed(ids);

        // then
        Assertions.assertThat(result).hasSize(2);
        Mockito.verify(delegate).filterConsumed(ids);
        Mockito.verify(duplicatedCounter).increment(2.0);
        Mockito.verify(consumedCounter).increment(0.0);
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
        Mockito.when(delegate.filterConsumed(ids)).thenReturn(alreadyConsumed);

        // when
        Set<UUID> result = decorator.filterConsumed(ids);

        // then
        Assertions.assertThat(result).hasSize(1);
        Mockito.verify(delegate).filterConsumed(ids);
        Mockito.verify(duplicatedCounter).increment(1.0);
        Mockito.verify(consumedCounter).increment(2.0);
    }

    @Test
    @DisplayName("UT filterConsumed() when empty set should not increment any counter")
    void filterConsumed_whenEmptySet_shouldNotIncrementAnyCounter() {
        // given
        Set<UUID> ids = Set.of();
        Set<UUID> alreadyConsumed = Set.of();
        Mockito.when(delegate.filterConsumed(ids)).thenReturn(alreadyConsumed);

        // when
        Set<UUID> result = decorator.filterConsumed(ids);

        // then
        Assertions.assertThat(result).isEmpty();
        Mockito.verify(delegate).filterConsumed(ids);
        Mockito.verify(duplicatedCounter).increment(0.0);
        Mockito.verify(consumedCounter).increment(0.0);
    }

    @Test
    @DisplayName("UT cleanBatchByTtl() when events cleaned should increment cleaned counter")
    void cleanBatchByTtl_whenEventsCleaned_shouldIncrementCleanedCounter() {
        // given
        Duration ttl = Duration.ofHours(1);
        int batchSize = 100;
        int cleanedCount = 50;
        Mockito.when(delegate.cleanBatchByTtl(ttl, batchSize)).thenReturn(cleanedCount);

        // when
        int result = decorator.cleanBatchByTtl(ttl, batchSize);

        // then
        Assertions.assertThat(result).isEqualTo(50);
        Mockito.verify(delegate).cleanBatchByTtl(ttl, batchSize);
        Mockito.verify(cleanedCounter).increment(50.0);
    }

    @Test
    @DisplayName("UT cleanBatchByTtl() when no events cleaned should increment cleaned counter with zero")
    void cleanBatchByTtl_whenNoEventsCleaned_shouldIncrementCleanedCounterWithZero() {
        // given
        Duration ttl = Duration.ofHours(1);
        int batchSize = 100;
        Mockito.when(delegate.cleanBatchByTtl(ttl, batchSize)).thenReturn(0);

        // when
        int result = decorator.cleanBatchByTtl(ttl, batchSize);

        // then
        Assertions.assertThat(result).isZero();
        Mockito.verify(delegate).cleanBatchByTtl(ttl, batchSize);
        Mockito.verify(cleanedCounter).increment(0.0);
    }
}