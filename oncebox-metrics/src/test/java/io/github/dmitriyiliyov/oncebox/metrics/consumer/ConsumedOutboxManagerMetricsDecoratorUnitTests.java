package io.github.dmitriyiliyov.oncebox.metrics.consumer;

import io.github.dmitriyiliyov.oncebox.core.consumer.ConsumedOutboxManager;
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

import static org.junit.jupiter.api.Assertions.assertThrows;

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
        Mockito.when(registry.counter(ArgumentMatchers.eq("consumed_outbox_events_total"), ArgumentMatchers.eq("type"), ArgumentMatchers.eq("rejected_duplicates")))
                .thenReturn(duplicatedCounter);
        Mockito.when(registry.counter(ArgumentMatchers.eq("consumed_outbox_events_total"), ArgumentMatchers.eq("type"), ArgumentMatchers.eq("consumed")))
                .thenReturn(consumedCounter);
        Mockito.when(registry.counter(ArgumentMatchers.eq("consumed_outbox_events_total"), ArgumentMatchers.eq("type"), ArgumentMatchers.eq("cleaned")))
                .thenReturn(cleanedCounter);

        decorator = new ConsumedOutboxManagerMetricsDecorator(registry, delegate);
    }

    @Test
    @DisplayName("UT constructor should throw NPE when registry is null")
    void constructor_shouldThrowNPE_whenRegistryIsNull() {
        assertThrows(NullPointerException.class, () -> new ConsumedOutboxManagerMetricsDecorator(null, delegate));
    }

    @Test
    @DisplayName("UT constructor should throw NPE when delegate is null")
    void constructor_shouldThrowNPE_whenDelegateIsNull() {
        assertThrows(NullPointerException.class, () -> new ConsumedOutboxManagerMetricsDecorator(registry, null));
    }

    @Test
    @DisplayName("UT isConsumed() when is consumed should increment counter")
    void isConsumed_whenEventTryConsume_shouldIncrementDuplicatedCounter() {
        UUID id = UUID.randomUUID();
        Mockito.when(delegate.tryConsume(id)).thenReturn(true);

        boolean result = decorator.tryConsume(id);

        Assertions.assertThat(result).isTrue();
        Mockito.verify(delegate).tryConsume(id);
        Mockito.verify(consumedCounter, Mockito.times(1)).increment();
        Mockito.verifyNoInteractions(duplicatedCounter);
    }

    @Test
    @DisplayName("UT isConsumed() when event is not consumed should increment duplicate counter")
    void isConsumed_whenEventTryNotConsumed_shouldIncrementConsumedCounter() {
        UUID id = UUID.randomUUID();
        Mockito.when(delegate.tryConsume(id)).thenReturn(false);

        boolean result = decorator.tryConsume(id);

        Assertions.assertThat(result).isFalse();
        Mockito.verify(delegate).tryConsume(id);
        Mockito.verify(consumedCounter, Mockito.never()).increment();
        Mockito.verify(duplicatedCounter, Mockito.times(1)).increment();
    }

    @Test
    @DisplayName("UT filterOutUnconsumed() when no duplicates should increment consumed counter with total count")
    void filterOutUnconsumed_whenNoDuplicates_shouldIncrementOutConsumedCounterWithTotalCount() {
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        Set<UUID> alreadyConsumed = Set.of();
        Mockito.when(delegate.tryConsumeAndGetDuplicates(ids)).thenReturn(alreadyConsumed);

        Set<UUID> result = decorator.tryConsumeAndGetDuplicates(ids);

        Assertions.assertThat(result).isEmpty();
        Mockito.verify(delegate).tryConsumeAndGetDuplicates(ids);
        Mockito.verifyNoInteractions(duplicatedCounter);
        Mockito.verify(consumedCounter).increment(3.0);
    }

    @Test
    @DisplayName("UT filterOutUnconsumed() when all duplicates should increment duplicated counter with total count")
    void tryConsumeAndGetDuplicates_whenAllDuplicates_shouldIncrementDuplicatedCounterWithTotalCount() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2);
        Set<UUID> alreadyConsumed = Set.of(id1, id2);
        Mockito.when(delegate.tryConsumeAndGetDuplicates(ids)).thenReturn(alreadyConsumed);

        Set<UUID> result = decorator.tryConsumeAndGetDuplicates(ids);

        Assertions.assertThat(result).hasSize(2);
        Mockito.verify(delegate).tryConsumeAndGetDuplicates(ids);
        Mockito.verify(duplicatedCounter).increment(2.0);
        Mockito.verify(consumedCounter).increment(0.0);
    }

    @Test
    @DisplayName("UT filterOutUnconsumed() when partial duplicates should increment both counters correctly")
    void tryConsumeAndGetDuplicates_whenPartialDuplicates_shouldIncrementBothCountersCorrectly() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2, id3);
        Set<UUID> alreadyConsumed = Set.of(id1);
        Mockito.when(delegate.tryConsumeAndGetDuplicates(ids)).thenReturn(alreadyConsumed);

        Set<UUID> result = decorator.tryConsumeAndGetDuplicates(ids);

        Assertions.assertThat(result).hasSize(1);
        Mockito.verify(delegate).tryConsumeAndGetDuplicates(ids);
        Mockito.verify(duplicatedCounter).increment(1.0);
        Mockito.verify(consumedCounter).increment(2.0);
    }

    @Test
    @DisplayName("UT filterOutUnconsumed() when empty set should not increment any counter")
    void tryConsumeAndGetDuplicates_whenEmptySet_shouldNotIncrementAnyCounter() {
        Set<UUID> ids = Set.of();
        Set<UUID> alreadyConsumed = Set.of();
        Mockito.when(delegate.tryConsumeAndGetDuplicates(ids)).thenReturn(alreadyConsumed);

        Set<UUID> result = decorator.tryConsumeAndGetDuplicates(ids);

        Assertions.assertThat(result).isEmpty();
        Mockito.verify(delegate).tryConsumeAndGetDuplicates(ids);
        Mockito.verifyNoInteractions(duplicatedCounter);
        Mockito.verify(consumedCounter).increment(0.0);
    }

    @Test
    @DisplayName("UT cleanBatchByTtl() when events cleaned should increment cleaned counter")
    void cleanBatchByTtl_whenEventsCleaned_shouldIncrementCleanedCounter() {
        Duration ttl = Duration.ofHours(1);
        int batchSize = 100;
        int cleanedCount = 50;
        Mockito.when(delegate.cleanBatchByTtl(ttl, batchSize)).thenReturn(cleanedCount);

        int result = decorator.cleanBatchByTtl(ttl, batchSize);

        Assertions.assertThat(result).isEqualTo(50);
        Mockito.verify(delegate).cleanBatchByTtl(ttl, batchSize);
        Mockito.verify(cleanedCounter).increment(50.0);
    }

    @Test
    @DisplayName("UT cleanBatchByTtl() when no events cleaned should increment cleaned counter with zero")
    void cleanBatchByTtl_whenNoEventsCleaned_shouldIncrementCleanedCounterWithZero() {
        Duration ttl = Duration.ofHours(1);
        int batchSize = 100;
        Mockito.when(delegate.cleanBatchByTtl(ttl, batchSize)).thenReturn(0);

        int result = decorator.cleanBatchByTtl(ttl, batchSize);

        Assertions.assertThat(result).isZero();
        Mockito.verify(delegate).cleanBatchByTtl(ttl, batchSize);
        Mockito.verify(cleanedCounter).increment(0.0);
    }
}