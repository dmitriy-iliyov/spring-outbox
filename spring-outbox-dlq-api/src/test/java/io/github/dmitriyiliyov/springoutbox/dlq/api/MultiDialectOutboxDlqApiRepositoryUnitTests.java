package io.github.dmitriyiliyov.springoutbox.dlq.api;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.core.utils.SqlIdHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultiDialectOutboxDlqApiRepositoryUnitTests {

    @Mock
    JdbcTemplate jdbcTemplate;

    @Mock
    SqlIdHelper idHelper;

    @Mock
    ResultSetMapper mapper;

    @InjectMocks
    MultiDialectOutboxDlqApiRepository tested;

    @Test
    @DisplayName("UT findBatch() when ids is empty should early return")
    void findBatch_whenIdsIsEmpty_shouldEarlyReturn() {
        // given
        Set<UUID> ids = Set.of();

        // when
        tested.findBatch(ids);

        // then
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT findBatch() when ids is null should throw NPE")
    void findBatch_whenIdsIsNull_shouldThrow() {
        // given
        Set<UUID> ids = null;

        // when + then
        assertThrows(NullPointerException.class, () -> tested.findBatch(ids));
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT count() should handle null result")
    void count_shouldHandleNullResult() {
        // given
        when(jdbcTemplate.queryForObject(any(), any(Class.class))).thenReturn(null);

        // when
        long result = tested.count();

        // then
        assertThat(result).isEqualTo(0L);
    }

    @Test
    @DisplayName("UT countByStatus() should handle null result")
    void countByStatus_shouldHandleNullResult() {
        // given
        when(jdbcTemplate.queryForObject(any(), any(Class.class), any(String.class))).thenReturn(null);

        // when
        long result = tested.countByStatus(DlqStatus.MOVED);

        // then
        assertThat(result).isEqualTo(0L);
    }

    @Test
    @DisplayName("UT updateBatchStatus() when ids is empty should early return")
    void updateBatchStatus_whenIdsIsEmpty_shouldEarlyReturn() {
        // given
        Set<UUID> ids = Set.of();
        DlqStatus status = DlqStatus.MOVED;

        // when
        tested.updateBatchStatus(ids, status);

        // then
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT updateBatchStatus() when ids is null should throw NPE")
    void updateBatchStatus_whenIdsIsNull_shouldThrow() {
        // given
        Set<UUID> ids = null;
        DlqStatus status = DlqStatus.MOVED;

        // when + then
        assertThrows(NullPointerException.class, () -> tested.updateBatchStatus(ids, status));
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids is empty should early return")
    void deleteBatch_whenIdsIsEmpty_shouldEarlyReturn() {
        // given
        Set<UUID> ids = Set.of();

        // when
        tested.deleteBatch(ids);

        // then
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids is null should throw NPE")
    void deleteBatch_whenIdsIsNull_shouldThrow() {
        // given
        Set<UUID> ids = null;

        // when + then
        assertThrows(NullPointerException.class, () -> tested.deleteBatch(ids));
        verifyNoInteractions(jdbcTemplate);
    }
}
