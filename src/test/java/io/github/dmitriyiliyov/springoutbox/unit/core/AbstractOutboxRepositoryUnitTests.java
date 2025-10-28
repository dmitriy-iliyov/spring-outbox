package io.github.dmitriyiliyov.springoutbox.unit.core;

import io.github.dmitriyiliyov.springoutbox.core.AbstractOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AbstractOutboxRepositoryUnitTests {

    @Mock
    JdbcTemplate jdbcTemplate;

    AbstractOutboxRepository tested;

    @BeforeEach
    void setUp() {
        tested = Mockito.spy(
                new AbstractOutboxRepository(jdbcTemplate) {
                    @Override
                    public List<OutboxEvent> findAndLockBatchByEventTypeAndStatus(String eventType, EventStatus status, int batchSize, EventStatus lockStatus) {
                        return List.of();
                    }

                    @Override
                    public List<OutboxEvent> findAndLockBatchByStatus(EventStatus status, int batchSize, EventStatus lockStatus) {
                        return List.of();
                    }

                    @Override
                    public void deleteBatchByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize) {

                    }

                    @Override
                    public int updateBatchStatusByStatus(EventStatus status, int batchSize, EventStatus newStatus) {
                        return 0;
                    }
                }
        );
    }

    @Test
    @DisplayName("UT updateBatchStatus() when all args are valid and EventStatus=PROCESSED, should update batch")
    public void updateBatchStatus_whenArgumentsValidAndStatusPROCESSED_shouldUpdate() {
        // given
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        EventStatus status = EventStatus.PROCESSED;

        // when
        tested.updateBatchStatus(ids, status);

        // then
        verify(jdbcTemplate, times(1)).update(anyString(), any(), any(), any(), any(), any());
        verifyNoMoreInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT updateBatchStatus() when all args are valid and EventStatus=IN_PROCESS, should update batch")
    public void updateBatchStatus_whenArgumentsValidAndStatusIN_PROCESS_shouldUpdate() {
        // given
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        EventStatus status = EventStatus.IN_PROCESS;

        // when
        tested.updateBatchStatus(ids, status);

        // then
        verify(jdbcTemplate, times(1)).update(anyString(), any(), any(), any(), any());
        verifyNoMoreInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT updateBatchStatus() when all args are valid and EventStatus=PENDING, should update batch")
    public void updateBatchStatus_whenArgumentsValidAndStatusPENDING_shouldUpdate() {
        // given
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        EventStatus status = EventStatus.PENDING;

        // when
        tested.updateBatchStatus(ids, status);

        // then
        verify(jdbcTemplate, times(1)).update(anyString(), any(), any(), any(), any());
        verifyNoMoreInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT updateBatchStatus() when all args are valid and EventStatus=FAILED, should update batch")
    public void updateBatchStatus_whenArgumentsValidAndStatusFAILED_shouldUpdate() {
        // given
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        EventStatus status = EventStatus.FAILED;

        // when + then
        assertThrows(IllegalArgumentException.class, () -> tested.updateBatchStatus(ids, status));

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT updateBatchStatus() when ids is empty should early return")
    public void updateBatchStatus_whenIdsIsEmpty_shouldEarlyReturn() {
        // given
        Set<UUID> ids = Set.of();
        EventStatus status = EventStatus.PENDING;

        // when
        tested.updateBatchStatus(ids, status);

        // then
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT updateBatchStatus() when ids is null should throws")
    public void updateBatchStatus_whenIdsIsNull_shouldThrows() {
        // given
        Set<UUID> ids = null;
        EventStatus status = EventStatus.PENDING;

        // when
        assertThrows(NullPointerException.class, () -> tested.updateBatchStatus(ids, status));

        // then
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT updateBatchStatus() when ids size is to big should early return")
    public void updateBatchStatus_whenIdsIsToBig_shouldEarlyReturn() {
        // given
        Set<UUID> ids = new HashSet<>(101);
        EventStatus status = EventStatus.PENDING;

        // when
        tested.updateBatchStatus(ids, status);

        // then
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT incrementRetryCountOrSetFailed() when ids is empty should early return")
    public void incrementRetryCountOrSetFailed_whenIdsIsEmpty_shouldEarlyReturn() {
        // given
        Set<UUID> ids = Set.of();
        int maxRetryCount = 1;

        // when
        tested.incrementRetryCountOrSetFailed(ids, maxRetryCount);

        // then
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT incrementRetryCountOrSetFailed() when ids is null should throws")
    public void incrementRetryCountOrSetFailed_whenIdsIsNull_shouldThrows() {
        // given
        Set<UUID> ids = null;
        int maxRetryCount = 1;

        // when
        assertThrows(NullPointerException.class, () -> tested.incrementRetryCountOrSetFailed(ids, maxRetryCount));

        // then
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT incrementRetryCountOrSetFailed() when ids size is to big should early return")
    public void incrementRetryCountOrSetFailed_whenIdsIsToBig_shouldEarlyReturn() {
        // given
        Set<UUID> ids = new HashSet<>(101);
        int maxRetryCount = 1;

        // when
        tested.incrementRetryCountOrSetFailed(ids, maxRetryCount);

        // then
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids is empty should early return")
    public void deleteBatch_whenIdsIsEmpty_shouldEarlyReturn() {
        // given
        Set<UUID> ids = Set.of();

        // when
        tested.deleteBatch(ids);

        // then
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids is null should throws")
    public void deleteBatch_whenIdsIsNull_shouldThrows() {
        // given
        Set<UUID> ids = null;
        EventStatus status = EventStatus.PENDING;

        // when
        assertThrows(NullPointerException.class, () -> tested.deleteBatch(ids));

        // then
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids size is to big should early return")
    public void deleteBatch_whenIdsIsToBig_shouldEarlyReturn() {
        // given
        Set<UUID> ids = new HashSet<>(101);

        // when
        tested.deleteBatch(ids);

        // then
        verifyNoInteractions(jdbcTemplate);
    }
}
