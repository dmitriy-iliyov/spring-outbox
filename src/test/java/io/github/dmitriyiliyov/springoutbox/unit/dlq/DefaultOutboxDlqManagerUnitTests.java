package io.github.dmitriyiliyov.springoutbox.unit.dlq;

import io.github.dmitriyiliyov.springoutbox.dlq.DefaultOutboxDlqManager;
import io.github.dmitriyiliyov.springoutbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.springoutbox.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.dlq.api.exception.OutboxDlqEventBatchNotFoundException;
import io.github.dmitriyiliyov.springoutbox.dlq.api.exception.OutboxDlqEventInProcessException;
import io.github.dmitriyiliyov.springoutbox.dlq.api.exception.OutboxDlqEventNotFoundException;
import io.github.dmitriyiliyov.springoutbox.dlq.dto.BatchUpdateRequest;
import io.github.dmitriyiliyov.springoutbox.utils.OutboxCache;
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
public class DefaultOutboxDlqManagerUnitTests {

    @Mock
    OutboxDlqRepository repository;

    @Mock
    OutboxCache<DlqStatus> cache;

    @InjectMocks
    DefaultOutboxDlqManager tested;

    @Test
    @DisplayName("UT saveBatch(), should early return if List.isEmpty")
    public void saveBatch_whenEventsIsEmpty_shouldEarlyReturn() {
        // given
        List<OutboxDlqEvent> events = List.of();

        // when
        tested.saveBatch(events);

        // then
        verifyNoInteractions(repository, cache);
    }

    @Test
    @DisplayName("UT saveBatch() when arg is valid, should saveBatch")
    public void saveBatch_whenArgIsValid_shouldEarlyReturn() {
        // given
        List<OutboxDlqEvent> events = List.of(mock(OutboxDlqEvent.class));

        // when
        tested.saveBatch(events);

        // then
        verify(repository, times(1)).saveBatch(events);
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
        OutboxDlqEventInProcessException e = assertThrows(OutboxDlqEventInProcessException.class, () -> tested.updateStatus(id, status));

        // then
        assertEquals("Outbox DLQ event with id=%s is IN_PROCESS, interaction impossible".formatted(id), e.getDetail());
        verify(repository, times(1)).findById(id);
        verifyNoMoreInteractions(repository, cache);
    }

    @Test
    @DisplayName("UT updateStatus() when event NOT equals DlqStatus.IN_PROCESS, should update")
    public void updateStatus_whenEventNotEqualsIN_PROCESS_shouldUpdate() {
        // given
        UUID id = UUID.randomUUID();
        DlqStatus status = DlqStatus.RESOLVED;
        OutboxDlqEvent event = mock(OutboxDlqEvent.class);
        when(event.getDlqStatus()).thenReturn(DlqStatus.NEW);

        when(repository.findById(id)).thenReturn(Optional.of(event));

        // when
        tested.updateStatus(id, status);

        // then
        verify(repository, times(1)).findById(id);
        verify(repository, times(1)).updateStatus(id, status);
        verifyNoMoreInteractions(repository, cache);
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

        when(repository.findBatch(ids)).thenReturn(List.of()); // пустой список, вызовет OutboxDlqEventBatchNotFoundException

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
        when(event.getDlqStatus()).thenReturn(DlqStatus.NEW);

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
        when(event.getDlqStatus()).thenReturn(DlqStatus.NEW);
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
        tested.deleteBatch(ids);

        // then
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids empty should early return")
    void deleteBatch_whenIdsEmpty_shouldEarlyReturn() {
        // given
        Set<UUID> ids = Set.of();

        // when
        tested.deleteBatch(ids);

        // then
        verifyNoInteractions(repository);
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
    @DisplayName("UT deleteBatch() when all events valid, should delete batch")
    void deleteBatch_whenEventsValid_shouldDelete() {
        // given
        UUID id = UUID.randomUUID();
        Set<UUID> ids = Set.of(id);
        OutboxDlqEvent event = mock(OutboxDlqEvent.class);
        when(event.getDlqStatus()).thenReturn(DlqStatus.NEW);

        when(repository.findBatch(ids)).thenReturn(List.of(event));

        // when
        tested.deleteBatch(ids);

        // then
        verify(repository).findBatch(ids);
        verify(repository).deleteBatch(ids);
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
            tested.deleteBatch(ids); // deleteBatch вызывает checkEventsAvailability
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
        OutboxDlqEventBatchNotFoundException e = assertThrows(OutboxDlqEventBatchNotFoundException.class, () -> tested.deleteBatch(ids));
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
        when(event1.getDlqStatus()).thenReturn(DlqStatus.NEW);

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
