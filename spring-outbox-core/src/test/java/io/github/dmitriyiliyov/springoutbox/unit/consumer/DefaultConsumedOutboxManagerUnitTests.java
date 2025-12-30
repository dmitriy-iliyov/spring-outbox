package io.github.dmitriyiliyov.springoutbox.unit.consumer;

import io.github.dmitriyiliyov.springoutbox.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.consumer.DefaultConsumedOutboxManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultConsumedOutboxManagerUnitTests {

    @Mock
    private ConsumedOutboxRepository repository;

    private DefaultConsumedOutboxManager manager;

    @BeforeEach
    void setUp() {
        manager = new DefaultConsumedOutboxManager(repository);
    }

    @Test
    @DisplayName("UT isConsumed() when event not consumed should return false and save event")
    void isConsumed_whenEventNotConsumed_shouldReturnFalseAndSaveEvent() {
        // given
        UUID id = UUID.randomUUID();
        when(repository.saveIfAbsent(id)).thenReturn(1);

        // when
        boolean result = manager.isConsumed(id);

        // then
        assertThat(result).isFalse();
        verify(repository).saveIfAbsent(id);
    }

    @Test
    @DisplayName("UT isConsumed() when event already consumed should return true")
    void isConsumed_whenEventAlreadyConsumed_shouldReturnTrue() {
        // given
        UUID id = UUID.randomUUID();
        when(repository.saveIfAbsent(id)).thenReturn(0);

        // when
        boolean result = manager.isConsumed(id);

        // then
        assertThat(result).isTrue();
        verify(repository).saveIfAbsent(id);
    }

    @Test
    @DisplayName("UT filterConsumed() when all events not consumed should return empty set")
    void filterConsumed_whenAllEventsNotConsumed_shouldReturnEmptySet() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2, id3);
        when(repository.saveIfAbsent(ids)).thenReturn(Set.of(id1, id2, id3));

        // when
        Set<UUID> result = manager.filterConsumed(ids);

        // then
        assertThat(result).isEmpty();
        verify(repository).saveIfAbsent(ids);
    }

    @Test
    @DisplayName("UT filterConsumed() when all events consumed should return all ids")
    void filterConsumed_whenAllEventsConsumed_shouldReturnAllIds() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2);
        when(repository.saveIfAbsent(ids)).thenReturn(Set.of());

        // when
        Set<UUID> result = manager.filterConsumed(ids);

        // then
        assertThat(result).containsExactlyInAnyOrder(id1, id2);
        verify(repository).saveIfAbsent(ids);
    }

    @Test
    @DisplayName("UT filterConsumed() when some events consumed should return consumed ids")
    void filterConsumed_whenSomeEventsConsumed_shouldReturnConsumedIds() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2, id3);
        when(repository.saveIfAbsent(ids)).thenReturn(Set.of(id1));

        // when
        Set<UUID> result = manager.filterConsumed(ids);

        // then
        assertThat(result).containsExactlyInAnyOrder(id2, id3);
        verify(repository).saveIfAbsent(ids);
    }

    @Test
    @DisplayName("UT filterConsumed() with empty set should return empty set")
    void filterConsumed_withEmptySet_shouldReturnEmptySet() {
        // given
        Set<UUID> ids = Set.of();
        when(repository.saveIfAbsent(ids)).thenReturn(Set.of());

        // when
        Set<UUID> result = manager.filterConsumed(ids);

        // then
        assertThat(result).isEmpty();
        verify(repository).saveIfAbsent(ids);
    }

    @Test
    @DisplayName("UT cleanBatchByTtl() when ttl is null should throw NullPointerException")
    void cleanBatchByTtl_whenTtlIsNull_shouldThrowNullPointerException() {
        // given
        Duration ttl = null;
        int batchSize = 100;

        // when + then
        assertThatThrownBy(() -> manager.cleanBatchByTtl(ttl, batchSize))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ttl cannot be null");
    }

    @Test
    @DisplayName("UT cleanBatchByTtl() should calculate correct threshold and delegate")
    void cleanBatchByTtl_shouldCalculateCorrectThresholdAndDelegate() {
        // given
        Duration ttl = Duration.ofHours(1);
        int batchSize = 100;
        when(repository.deleteBatchByThreshold(any(Instant.class), eq(batchSize))).thenReturn(50);

        // when
        int result = manager.cleanBatchByTtl(ttl, batchSize);

        // then
        assertThat(result).isEqualTo(50);
        ArgumentCaptor<Instant> thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(repository).deleteBatchByThreshold(thresholdCaptor.capture(), eq(batchSize));

        Instant threshold = thresholdCaptor.getValue();
        Instant expectedThreshold = Instant.now().minusSeconds(ttl.toSeconds());
        assertThat(threshold).isBetween(
                expectedThreshold.minusSeconds(1),
                expectedThreshold.plusSeconds(1)
        );
    }

    @Test
    @DisplayName("UT cleanBatchByTtl() when no events cleaned should return zero")
    void cleanBatchByTtl_whenNoEventsCleaned_shouldReturnZero() {
        // given
        Duration ttl = Duration.ofHours(24);
        int batchSize = 100;
        when(repository.deleteBatchByThreshold(any(Instant.class), eq(batchSize))).thenReturn(0);

        // when
        int result = manager.cleanBatchByTtl(ttl, batchSize);

        // then
        assertThat(result).isZero();
        verify(repository).deleteBatchByThreshold(any(Instant.class), eq(batchSize));
    }

    @Test
    @DisplayName("UT cleanBatchByTtl() with different ttl values should calculate correct threshold")
    void cleanBatchByTtl_withDifferentTtlValues_shouldCalculateCorrectThreshold() {
        // given
        Duration ttl = Duration.ofMinutes(30);
        int batchSize = 50;
        when(repository.deleteBatchByThreshold(any(Instant.class), eq(batchSize))).thenReturn(25);

        // when
        int result = manager.cleanBatchByTtl(ttl, batchSize);

        // then
        assertThat(result).isEqualTo(25);
        ArgumentCaptor<Instant> thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(repository).deleteBatchByThreshold(thresholdCaptor.capture(), eq(batchSize));

        Instant threshold = thresholdCaptor.getValue();
        Instant expectedThreshold = Instant.now().minusSeconds(1800);
        assertThat(threshold).isBetween(
                expectedThreshold.minusSeconds(1),
                expectedThreshold.plusSeconds(1)
        );
    }

    @Test
    @DisplayName("UT cleanBatchByTtl() with zero ttl should use current time as threshold")
    void cleanBatchByTtl_withZeroTtl_shouldUseCurrentTimeAsThreshold() {
        // given
        Duration ttl = Duration.ZERO;
        int batchSize = 100;
        when(repository.deleteBatchByThreshold(any(Instant.class), eq(batchSize))).thenReturn(10);

        // when
        int result = manager.cleanBatchByTtl(ttl, batchSize);

        // then
        assertThat(result).isEqualTo(10);
        ArgumentCaptor<Instant> thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(repository).deleteBatchByThreshold(thresholdCaptor.capture(), eq(batchSize));

        Instant threshold = thresholdCaptor.getValue();
        Instant now = Instant.now();
        assertThat(threshold).isBetween(
                now.minusSeconds(1),
                now.plusSeconds(1)
        );
    }
}