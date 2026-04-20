package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.core.utils.SqlIdHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AbstractOutboxDlqRepositoryUnitTests {

    @Mock
    JdbcTemplate jdbcTemplate;

    @Mock
    SqlIdHelper idHelper;

    @Mock
    ResultSetMapper mapper;

    AbstractOutboxDlqRepository tested;

    @BeforeEach
    void setUp() {
        tested = Mockito.spy(
                new AbstractOutboxDlqRepository(jdbcTemplate, idHelper, mapper) {
                    @Override
                    public List<OutboxDlqEvent> findAndLockBatchByStatus(DlqStatus status, int batchSize, DlqStatus lockStatus) {
                        return List.of();
                    }
                }
        );
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
