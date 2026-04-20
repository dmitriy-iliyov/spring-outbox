package io.github.dmitriyiliyov.springoutbox.web;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.springoutbox.web.exception.OutboxDlqEventBatchNotFoundException;
import io.github.dmitriyiliyov.springoutbox.web.exception.OutboxDlqEventInProcessException;
import io.github.dmitriyiliyov.springoutbox.web.exception.OutboxDlqEventNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class DefaultOutboxDlqWebManagerUnitTests {

    @Mock
    OutboxDlqWebRepository repository;

    @InjectMocks
    DefaultOutboxDlqWebManager tested;

    @Test
    @DisplayName("UT findById() when event found should return event")
    void findById_whenFound_shouldReturnEvent() {
        // given
        UUID id = UUID.randomUUID();
        OutboxDlqEvent event = mock(OutboxDlqEvent.class);
        when(repository.findById(id)).thenReturn(Optional.of(event));

        // when
        OutboxDlqEvent result = tested.findById(id);

        // then
        assertEquals(event, result);
        verify(repository).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT findById() when event not found should throw OutboxDlqEventNotFoundException")
    void findById_whenNotFound_shouldThrow() {
        // given
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        // when + then
        assertThrows(OutboxDlqEventNotFoundException.class, () -> tested.findById(id));
        verify(repository).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT findBatch() should call repository findBatchByStatus and return list")
    void findBatch_shouldCallRepositoryAndReturnList() {
        // given
        BatchRequest request = mock(BatchRequest.class);
        when(request.status()).thenReturn(DlqStatus.RESOLVED);
        when(request.batchNumber()).thenReturn(1);
        when(request.batchSize()).thenReturn(10);

        List<OutboxDlqEvent> expectedEvents = List.of(mock(OutboxDlqEvent.class));
        when(repository.findBatchByStatus(DlqStatus.RESOLVED, 1, 10)).thenReturn(expectedEvents);

        // when
        List<OutboxDlqEvent> result = tested.findBatch(request);

        // then
        assertEquals(expectedEvents, result);
        verify(repository).findBatchByStatus(DlqStatus.RESOLVED, 1, 10);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT count() when status is null should call repository.count()")
    void count_whenStatusIsNull_shouldCallRepositoryCount() {
        // given
        when(repository.count()).thenReturn(15L);

        // when
        long result = tested.count(null);

        // then
        assertEquals(15L, result);
        verify(repository).count();
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT count() when status is not null should call repository.countByStatus()")
    void count_whenStatusIsNotNull_shouldCallRepositoryCountByStatus() {
        // given
        DlqStatus status = DlqStatus.MOVED;
        when(repository.countByStatus(status)).thenReturn(5L);

        // when
        long result = tested.count(status);

        // then
        assertEquals(5L, result);
        verify(repository).countByStatus(status);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT updateStatus() when event equals DlqStatus.IN_PROCESS, should trows")
    public void updateStatus_whenEventEqualsIN_PROCESS_shouldTrows() {
        // given
        UUID id = UUID.randomUUID();
        DlqStatus status = DlqStatus.IN_PROCESS;
        OutboxDlqEvent event = mock(OutboxDlqEvent.class);
        when(event.getDlqStatus()).thenReturn(DlqStatus.IN_PROCESS);
        when(event.getId()).thenReturn(id);

        when(repository.findById(id)).thenReturn(Optional.of(event));

        // when
        OutboxDlqEventInProcessException e = assertThrows(
                OutboxDlqEventInProcessException.class, () -> tested.updateStatus(id, status)
        );

        // then
        assertEquals("Outbox DLQ event with id=%s is IN_PROCESS, interaction impossible".formatted(id), e.getDetail());
        verify(repository, times(1)).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT updateStatus() when event NOT equals DlqStatus.IN_PROCESS, should update")
    public void updateStatus_whenEventNotEqualsIN_PROCESS_shouldUpdate() {
        // given
        UUID id = UUID.randomUUID();
        DlqStatus status = DlqStatus.RESOLVED;
        OutboxDlqEvent event = mock(OutboxDlqEvent.class);
        when(event.getDlqStatus()).thenReturn(DlqStatus.MOVED);

        when(repository.findById(id)).thenReturn(Optional.of(event));

        // when
        tested.updateStatus(id, status);

        // then
        verify(repository, times(1)).findById(id);
        verify(repository, times(1)).updateStatus(id, status);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT updateBatchStatus() when ids is null should early return")
    void updateBatchStatus_whenIdsIsNull_shouldEarlyReturn() {
        // given
        BatchUpdateRequest request = new BatchUpdateRequest(null, DlqStatus.RESOLVED);

        // when
        tested.updateBatchStatus(request);

        // then
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT updateBatchStatus() when ids is empty should early return")
    void updateBatchStatus_whenIdsIsEmpty_shouldEarlyReturn() {
        // given
        BatchUpdateRequest request = new BatchUpdateRequest(Set.of(), DlqStatus.RESOLVED);

        // when
        tested.updateBatchStatus(request);

        // then
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT updateBatchStatus() when checkEventsAvailability throws, should propagate exception")
    void updateBatchStatus_whenCheckEventsAvailabilityFails_shouldThrow() {
        // given
        UUID id = UUID.randomUUID();
        Set<UUID> ids = Set.of(id);
        BatchUpdateRequest request = new BatchUpdateRequest(ids, DlqStatus.RESOLVED);

        when(repository.findBatch(ids)).thenReturn(List.of());

        // when + then
        assertThrows(OutboxDlqEventBatchNotFoundException.class, () -> tested.updateBatchStatus(request));

        verify(repository, times(1)).findBatch(ids);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT updateBatchStatus() when all ids valid, should update batch status")
    void updateBatchStatus_whenIdsValid_shouldUpdate() {
        // given
        UUID id = UUID.randomUUID();
        Set<UUID> ids = Set.of(id);
        BatchUpdateRequest request = new BatchUpdateRequest(ids, DlqStatus.RESOLVED);
        OutboxDlqEvent event = mock(OutboxDlqEvent.class);
        when(event.getDlqStatus()).thenReturn(DlqStatus.MOVED);

        when(repository.findBatch(ids)).thenReturn(List.of(event));

        // when
        tested.updateBatchStatus(request);

        // then
        verify(repository).findBatch(ids);
        verify(repository).updateBatchStatus(ids, DlqStatus.RESOLVED);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteById() when event not found, should throw OutboxDlqEventNotFoundException")
    void deleteById_whenEventNotFound_shouldThrow() {
        // given
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        // when + then
        assertThrows(OutboxDlqEventNotFoundException.class, () -> tested.deleteById(id));
        verify(repository).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteById() when event in IN_PROCESS, should throw OutboxDlqEventInProcessException")
    void deleteById_whenEventInProcess_shouldThrow() {
        // given
        UUID id = UUID.randomUUID();
        OutboxDlqEvent event = mock(OutboxDlqEvent.class);
        when(event.getDlqStatus()).thenReturn(DlqStatus.IN_PROCESS);
        when(event.getId()).thenReturn(id);
        when(repository.findById(id)).thenReturn(Optional.of(event));

        // when + then
        assertThrows(OutboxDlqEventInProcessException.class, () -> tested.deleteById(id));
        verify(repository).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteById() when event valid, should delete")
    void deleteById_whenEventValid_shouldDelete() {
        // given
        UUID id = UUID.randomUUID();
        OutboxDlqEvent event = mock(OutboxDlqEvent.class);
        when(event.getDlqStatus()).thenReturn(DlqStatus.MOVED);
        when(repository.findById(id)).thenReturn(Optional.of(event));

        // when
        tested.deleteById(id);

        // then
        verify(repository).findById(id);
        verify(repository).deleteById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids null should early return")
    void deleteBatch_whenIdsNull_shouldEarlyReturn() {
        // given
        Set<UUID> ids = null;

        // when
        int deleteCount = tested.deleteBatch(ids);

        // then
        assertEquals(0, deleteCount);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids empty should early return")
    void deleteBatch_whenIdsEmpty_shouldEarlyReturn() {
        // given
        Set<UUID> ids = Set.of();

        // when
        int deleteCount = tested.deleteBatch(ids);

        // then
        assertEquals(0, deleteCount);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteBatch() when all events valid, should delete batch")
    void deleteBatch_whenEventsValid_shouldDelete() {
        // given
        UUID id = UUID.randomUUID();
        Set<UUID> ids = Set.of(id);
        OutboxDlqEvent event = mock(OutboxDlqEvent.class);
        when(event.getDlqStatus()).thenReturn(DlqStatus.MOVED);
        List<OutboxDlqEvent> dlqEvents = List.of(event);
        when(repository.findBatch(ids)).thenReturn(dlqEvents);
        when(repository.deleteBatch(ids)).thenReturn(ids.size());

        // when
        int deleteCount = tested.deleteBatch(ids);

        // then
        assertEquals(ids.size(), deleteCount);
        verify(repository).deleteBatch(ids);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteBatch() when checkEventsAvailability throws, should propagate exception")
    void deleteBatch_whenCheckEventsAvailabilityFails_shouldThrow() {
        // given
        UUID id = UUID.randomUUID();
        Set<UUID> ids = Set.of(id);
        when(repository.findBatch(ids)).thenReturn(List.of());

        // when + then
        assertThrows(OutboxDlqEventBatchNotFoundException.class, () -> tested.deleteBatch(ids));
        verify(repository).findBatch(ids);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT checkEventsAvailability(), events null should throw OutboxDlqEventBatchNotFoundException")
    void checkEventsAvailability_whenEventsNull_shouldThrow() {
        // given
        UUID id = UUID.randomUUID();
        Set<UUID> ids = Set.of(id);
        when(repository.findBatch(ids)).thenReturn(null);

        // when + then
        assertThrows(OutboxDlqEventBatchNotFoundException.class, () -> {
            tested.deleteBatch(ids);
        });

        verify(repository).findBatch(ids);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT checkEventsAvailability(), events empty should throw OutboxDlqEventBatchNotFoundException")
    void checkEventsAvailability_whenEventsEmpty_shouldThrow() {
        // given
        UUID id = UUID.randomUUID();
        Set<UUID> ids = Set.of(id);
        when(repository.findBatch(ids)).thenReturn(List.of());

        // when + then
        assertThrows(OutboxDlqEventBatchNotFoundException.class, () -> tested.deleteBatch(ids));

        verify(repository).findBatch(ids);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT checkEventsAvailability(), size mismatch should throw OutboxDlqEventBatchNotFoundException with missing ids")
    void checkEventsAvailability_whenSizeMismatch_shouldThrow() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Set<UUID> ids = new HashSet<>(Set.of(id1, id2));

        OutboxDlqEvent event = mock(OutboxDlqEvent.class);
        when(event.getId()).thenReturn(id1);

        when(repository.findBatch(ids)).thenReturn(List.of(event));

        // when + then
        OutboxDlqEventBatchNotFoundException e = assertThrows(
                OutboxDlqEventBatchNotFoundException.class, () -> tested.deleteBatch(ids)
        );
        assertEquals(Set.of(id2), e.getNotFoundIds());

        verify(repository).findBatch(ids);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT checkEventsAvailability(), any event IN_PROCESS should throw OutboxDlqEventInProcessException")
    void checkEventsAvailability_whenEventInProcess_shouldThrow() {
        // given
        UUID id = UUID.randomUUID();
        Set<UUID> ids = Set.of(id);

        OutboxDlqEvent event = mock(OutboxDlqEvent.class);
        when(event.getId()).thenReturn(id);
        when(event.getDlqStatus()).thenReturn(DlqStatus.IN_PROCESS);

        when(repository.findBatch(ids)).thenReturn(List.of(event));

        // when + then
        assertThrows(OutboxDlqEventInProcessException.class, () -> tested.deleteBatch(ids));

        verify(repository).findBatch(ids);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("UT checkEventsAvailability(), all events valid should pass without exception")
    void checkEventsAvailability_whenAllValid_shouldPass() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2);

        OutboxDlqEvent event1 = mock(OutboxDlqEvent.class);
        when(event1.getDlqStatus()).thenReturn(DlqStatus.MOVED);

        OutboxDlqEvent event2 = mock(OutboxDlqEvent.class);
        when(event2.getDlqStatus()).thenReturn(DlqStatus.RESOLVED);

        when(repository.findBatch(ids)).thenReturn(List.of(event1, event2));

        // when
        tested.deleteBatch(ids);

        // then
        verify(repository).findBatch(ids);
        verify(repository).deleteBatch(ids);
        verifyNoMoreInteractions(repository);
    }
}
