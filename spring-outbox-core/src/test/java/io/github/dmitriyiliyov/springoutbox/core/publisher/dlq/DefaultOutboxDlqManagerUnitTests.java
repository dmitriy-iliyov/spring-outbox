package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class DefaultOutboxDlqManagerUnitTests {

    @Mock
    OutboxDlqRepository repository;

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
        verifyNoInteractions(repository);
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
        when(repository.deleteBatch(ids)).thenReturn(ids.size());

        // when
        int deleteCount = tested.deleteBatch(ids);

        // then
        assertEquals(ids.size(), deleteCount);
        verify(repository).deleteBatch(ids);
        verifyNoMoreInteractions(repository);
    }
}
