package io.github.dmitriyiliyov.springoutbox.unit.dlq;

import io.github.dmitriyiliyov.springoutbox.dlq.AbstractOutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.dlq.OutboxDlqEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class AbstractOutboxDlqRepositoryUnitTests {

    @Mock
    JdbcTemplate jdbcTemplate;

    AbstractOutboxDlqRepository tested;

    @BeforeEach
    public void init() {
        tested = Mockito.spy(
                new AbstractOutboxDlqRepository(jdbcTemplate) {
                    @Override
                    public List<OutboxDlqEvent> findAndLockBatchByStatus(DlqStatus status, int batchSize, DlqStatus lockStatus) {
                        return List.of();
                    }

                    @Override
                    public List<OutboxDlqEvent> findBatchByStatus(DlqStatus status, int batchSize) {
                        return List.of();
                    }
                }
        );
    }

    // 13 41
    @Test
    @DisplayName("UT findBatch() when ids is null, should throws")
    public void findBatch_whenIdsIsNull_shouldTrows() {
        // given
        Set<UUID> ids = null;

        // when + then
        assertThrows(NullPointerException.class, () -> tested.findBatch(ids));

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT findBatch() when ids is empty, should early return")
    public void findBatch_whenIdsIsEmpty_shouldEarlyReturn() {
        // given
        Set<UUID> ids = Set.of();

        // when
        List<OutboxDlqEvent> result = tested.findBatch(ids);

        //then
        assertTrue(result.isEmpty());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT findBatch() when ids is to large, should early return")
    public void findBatch_whenIdsIoToLarge_shouldEarlyReturn() {
        // given
        Set<UUID> ids = new HashSet<>();
        for (int i = 0; i < 101; i++) {
            ids.add(UUID.randomUUID());
        }

        // when
        List<OutboxDlqEvent> result = tested.findBatch(ids);

        //then
        assertTrue(result.isEmpty());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT updateBatchStatus() when ids is null, should throws")
    public void updateBatchStatus_whenIdsIsNull_shouldTrows() {
        // given
        Set<UUID> ids = null;
        DlqStatus status = DlqStatus.IN_PROCESS;

        // when + then
        assertThrows(NullPointerException.class, () -> tested.updateBatchStatus(ids, status));

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT updateBatchStatus() when ids is empty, should early return")
    public void updateBatchStatus_whenIdsIsEmpty_shouldEarlyReturn() {
        // given
        Set<UUID> ids = Set.of();
        DlqStatus status = DlqStatus.IN_PROCESS;

        // when
        tested.updateBatchStatus(ids, status);

        //then
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT updateBatchStatus() when ids is to large, should early return")
    public void updateBatchStatus_whenIdsIoToLarge_shouldEarlyReturn() {
        // given
        Set<UUID> ids = new HashSet<>();
        for (int i = 0; i < 101; i++) {
            ids.add(UUID.randomUUID());
        }
        DlqStatus status = DlqStatus.IN_PROCESS;

        // when
        tested.updateBatchStatus(ids, status);

        //then
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids is null, should throws")
    public void deleteBatch_whenIdsIsNull_shouldTrows() {
        // given
        Set<UUID> ids = null;

        // when + then
        assertThrows(NullPointerException.class, () -> tested.deleteBatch(ids));

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids is empty, should early return")
    public void deleteBatch_whenIdsIsEmpty_shouldEarlyReturn() {
        // given
        Set<UUID> ids = Set.of();
        // when
        tested.deleteBatch(ids);

        //then
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids is to large, should early return")
    public void deleteBatch_whenIdsIoToLarge_shouldEarlyReturn() {
        // given
        Set<UUID> ids = new HashSet<>();
        for (int i = 0; i < 101; i++) {
            ids.add(UUID.randomUUID());
        }

        // when
        tested.deleteBatch(ids);

        //then
        verifyNoInteractions(jdbcTemplate);
    }
}
