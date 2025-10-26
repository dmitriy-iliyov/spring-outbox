package io.github.dmitriyiliyov.springoutbox.unit.core;

import io.github.dmitriyiliyov.springoutbox.core.DefaultOutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.OutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.utils.OutboxCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DefaultOutboxManagerUnitTests {

    @Mock
    OutboxRepository repository;

    @Mock
    OutboxCache<EventStatus> cache;

    @InjectMocks
    DefaultOutboxManager tested;

    @Test
    @DisplayName("UT loadBatch(String, int) when events is not empty, should update status and return event list")
    public void loadBatch_whenEventsIsNotEmpty_shouldUpdateStatusAndReturnEvents() {
        // given
        String eventType = "event-type";
        int batchSize = 10;
        EventStatus lockStatus = EventStatus.IN_PROCESS;

        OutboxEvent event1 = mock(OutboxEvent.class);
        OutboxEvent event2 = mock(OutboxEvent.class);
        List<OutboxEvent> eventMocks = List.of(event1, event2);

        when(repository.findAndLockBatchByEventTypeAndStatus(eventType, EventStatus.PENDING, batchSize, lockStatus))
                .thenReturn(eventMocks);

        // when
        List<OutboxEvent> result = tested.loadBatch(eventType, batchSize);

        // then
        assertThat(result)
                .hasSize(2)
                .containsExactly(event1, event2);

        verify(repository, times(1))
                .findAndLockBatchByEventTypeAndStatus(eventType, EventStatus.PENDING, batchSize, lockStatus);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT loadBatch(String, int) when events is empty, should not update status and return empty event list")
    public void loadBatch_whenEventsIsEmpty_shouldReturnEmptyList() {
        // given
        String eventType = "event-type";
        int batchSize = 10;
        EventStatus lockStatus = EventStatus.IN_PROCESS;

        when(repository.findAndLockBatchByEventTypeAndStatus(eventType, EventStatus.PENDING, batchSize, lockStatus))
                .thenReturn(List.of());

        // when
        List<OutboxEvent> result = tested.loadBatch(eventType, batchSize);

        // then
        assertTrue(result.isEmpty());

        verify(repository, times(1))
                .findAndLockBatchByEventTypeAndStatus(eventType, EventStatus.PENDING, batchSize, lockStatus);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT loadBatch(EventStatus, int, String) when events is not empty, should update status and return event list")
    public void loadBatch2_whenEventsIsNotEmpty_shouldUpdateStatusAndReturnEvents() {
        // given
        EventStatus status = EventStatus.FAILED;
        int batchSize = 10;
        EventStatus lockStatus = EventStatus.IN_PROCESS;

        OutboxEvent event1 = mock(OutboxEvent.class);
        OutboxEvent event2 = mock(OutboxEvent.class);
        List<OutboxEvent> eventMocks = List.of(event1, event2);

        when(repository.findAndLockBatchByStatus(status, batchSize, lockStatus)).thenReturn(eventMocks);

        // when
        List<OutboxEvent> result = tested.loadBatch(status, batchSize);

        // then
        assertThat(result)
                .hasSize(2)
                .containsExactly(event1, event2);

        verify(repository, times(1)).findAndLockBatchByStatus(status, batchSize, lockStatus);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT loadBatch(EventStatus, int, String) when events is empty, should not update status and return empty event list")
    public void loadBatch2_whenEventsIsEmpty_shouldReturnEmptyList() {
        // given
        EventStatus status = EventStatus.FAILED;
        int batchSize = 10;
        EventStatus lockStatus = EventStatus.IN_PROCESS;

        when(repository.findAndLockBatchByStatus(status, batchSize, lockStatus)).thenReturn(List.of());

        // when
        List<OutboxEvent> result = tested.loadBatch(status, batchSize);

        // then
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findAndLockBatchByStatus(status, batchSize, lockStatus);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT finalizeBatch() when maxRetryCount < 0, should throw")
    public void finalizeBatch_whenMaxRetryCountNegative_shouldThrow() {
        // given
        Set<UUID> processedIds = Set.of(UUID.randomUUID());
        Set<UUID> failedIds = Set.of(UUID.randomUUID());
        int maxRetryCount = -1;

        // then
        assertThrows(IllegalArgumentException.class,
                () -> tested.finalizeBatch(processedIds, failedIds, maxRetryCount));

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT finalizeBatch() when params are valid")
    public void finalizeBatch_whenParamsAreValid_shouldUpdateAndIncrement() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Set<UUID> processedIds = new HashSet<>(Set.of(id1));
        Set<UUID> failedIds = new HashSet<>(Set.of(id2));
        int maxRetryCount = 3;

        // when
        tested.finalizeBatch(processedIds, failedIds, maxRetryCount);

        // then
        ArgumentCaptor<Set<UUID>> processedIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(repository, times(1))
                .updateBatchStatus(processedIdsCaptor.capture(), eq(EventStatus.PROCESSED));
        assertThat(processedIdsCaptor.getValue()).containsExactlyInAnyOrder(id1);

        ArgumentCaptor<Set<UUID>> failedIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(repository, times(1))
                .incrementRetryCountOrSetFailed(failedIdsCaptor.capture(), eq(maxRetryCount));
        assertThat(failedIdsCaptor.getValue()).containsExactlyInAnyOrder(id2);

        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT finalizeBatch() when id sets are overlapping, should remove overlap")
    public void finalizeBatch_whenSetOverlap_shouldRemoveOverlap() {
        // given
        UUID common = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Set<UUID> processedIds = new HashSet<>(Set.of(common, id1));
        Set<UUID> failedIds = new HashSet<>(Set.of(common, id2));
        int maxRetryCount = 3;

        // when
        tested.finalizeBatch(processedIds, failedIds, maxRetryCount);

        // then
        ArgumentCaptor<Set<UUID>> processedIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(repository, times(1))
                .updateBatchStatus(processedIdsCaptor.capture(), eq(EventStatus.PROCESSED));
        assertThat(processedIdsCaptor.getValue()).containsExactlyInAnyOrder(id1);

        ArgumentCaptor<Set<UUID>> failedIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(repository, times(1))
                .incrementRetryCountOrSetFailed(failedIdsCaptor.capture(), eq(maxRetryCount));
        assertThat(failedIdsCaptor.getValue()).containsExactlyInAnyOrder(id2, common);

        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT finalizeBatch() when processed ids set is empty after remove overlapped id, should not call repository to upd to PROCESSED")
    public void finalizeBatch_whenProcessedIsEmptyAfterRemoveOverlap_shouldNotUpdToPROCESSED() {
        // given
        UUID common = UUID.randomUUID();
        Set<UUID> processedIds = new HashSet<>(Set.of(common));
        Set<UUID> failedIds = new HashSet<>(Set.of(common));
        int maxRetryCount = 2;

        // when
        tested.finalizeBatch(processedIds, failedIds, maxRetryCount);

        // then
        verify(repository, never()).updateBatchStatus(any(), eq(EventStatus.PROCESSED));
        ArgumentCaptor<Set<UUID>> failedIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(repository, times(1))
                .incrementRetryCountOrSetFailed(failedIdsCaptor.capture(), eq(maxRetryCount));
        assertThat(failedIdsCaptor.getValue()).containsExactlyInAnyOrder(common);

        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT finalizeBatch() when processed ids is empty")
    public void finalizeBatch_whenProcessedIdsIsEmpty_shouldNotUpdateToPROCESSED() {
        // given
        Set<UUID> processedIds = Collections.emptySet();
        Set<UUID> failedIds = Set.of(UUID.randomUUID());

        // when
        tested.finalizeBatch(processedIds, failedIds, 1);

        // then
        verify(repository, never()).updateBatchStatus(any(), eq(EventStatus.PROCESSED));
        verify(repository, times(1)).incrementRetryCountOrSetFailed(failedIds, 1);

        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT finalizeBatch() when processed ids is null")
    public void finalizeBatch_whenProcessedIdsIsNull_shouldNotUpdateToPROCESSED() {
        // given
        Set<UUID> failedIds = Set.of(UUID.randomUUID());

        // when
        tested.finalizeBatch(null, failedIds, 1);

        // then
        verify(repository, never()).updateBatchStatus(any(), eq(EventStatus.PROCESSED));
        verify(repository, times(1)).incrementRetryCountOrSetFailed(failedIds, 1);

        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT finalizeBatch() when failed ids is empty")
    public void finalizeBatch_whenFailedIdsIsEmpty_shouldNotIncrement() {
        // given
        Set<UUID> processedIds = Set.of(UUID.randomUUID());
        Set<UUID> failedIds = Collections.emptySet();

        // when
        tested.finalizeBatch(processedIds, failedIds, 1);

        // then
        verify(repository, times(1)).updateBatchStatus(processedIds, EventStatus.PROCESSED);
        verify(repository, never()).incrementRetryCountOrSetFailed(any(), anyInt());

        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT finalizeBatch() when failed ids is null")
    public void finalizeBatch_whenFailedIdsIsNull_shouldNotIncrement() {
        // given
        Set<UUID> processedIds = Set.of(UUID.randomUUID());

        // when
        tested.finalizeBatch(processedIds, null, 1);

        // then
        verify(repository, times(1)).updateBatchStatus(processedIds, EventStatus.PROCESSED);
        verify(repository, never()).incrementRetryCountOrSetFailed(any(), anyInt());

        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids not null and not empty should delete")
    public void delete_whenIdsValid_shouldDelete() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2);

        // when
        tested.deleteBatch(ids);

        // then
        ArgumentCaptor<Set<UUID>> idsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(repository, times(1)).deleteBatch(idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(id1, id2);

        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids null should not delete")
    public void delete_whenIdsIsNull_shouldNotDelete() {
        // given
        Set<UUID> ids = null;

        // when
        tested.deleteBatch(ids);

        // then
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids is empty should not delete")
    public void delete_whenIdsIsEmpty_shouldNotDelete() {
        // given
        Set<UUID> ids = Set.of();

        // when
        tested.deleteBatch(ids);

        // then
        verifyNoInteractions(repository);
    }
}
