package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.it.BaseOracleIntegrationTests;
import io.github.dmitriyiliyov.springoutbox.core.utils.DefaultBytesSqlResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.core.utils.OracleSqlIdHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class OracleOutboxDlqRepositoryIntegrationTests extends BaseOracleIntegrationTests {

    private final OutboxDlqRepository repository;
    private final AbstractOutboxDlqRepositoryIntegrationTests delegate;

    public OracleOutboxDlqRepositoryIntegrationTests(
            @Qualifier("oracleOutboxDlqRepository") OutboxDlqRepository repository,
            @Qualifier("oracleJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        this.repository = repository;
        this.delegate = new AbstractOutboxDlqRepositoryIntegrationTests(
                repository,
                jdbcTemplate,
                new OracleSqlIdHelper(),
                new DefaultBytesSqlResultSetMapper()
        );
    }

    @Test
    @DisplayName("IT saveBatch() with single event should persist correctly")
    void saveBatch_singleEvent_persistedCorrectly() {
        delegate.saveBatch_singleEvent_persistedCorrectly();
    }

    @Test
    @DisplayName("IT saveBatch() with multiple events should persist all")
    void saveBatch_multipleEvents_allPersisted() {
        delegate.saveBatch_multipleEvents_allPersisted();
    }

    @Test
    @DisplayName("IT deleteBatch() for existing ids should delete and return count")
    void deleteBatch_existingIds_deletedAndReturnsCount() {
        delegate.deleteBatch_existingIds_deletedAndReturnsCount();
    }

    @Test
    @DisplayName("IT deleteBatch() with empty ids should return zero")
    void deleteBatch_emptyIds_returnsZero() {
        delegate.deleteBatch_emptyIds_returnsZero();
    }

    @Test
    @DisplayName("IT deleteBatch() should not affect other events")
    void deleteBatch_doesNotAffectOtherEvents() {
        delegate.deleteBatch_doesNotAffectOtherEvents();
    }

    @Test
    @DisplayName("IT findAndLockBatchByStatus() should lock and set status")
    void findAndLockBatchByStatus_locksAndSetsStatus() {
        OutboxDlqEvent e1 = delegate.buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent e2 = delegate.buildEvent(DlqStatus.MOVED);
        repository.saveBatch(List.of(e1, e2));

        List<OutboxDlqEvent> locked = repository
                .findAndLockBatchByStatus(DlqStatus.MOVED, 10, DlqStatus.IN_PROCESS);

        assertThat(locked)
                .hasSize(2)
                .extracting(OutboxDlqEvent::getId)
                .containsExactlyInAnyOrder(e1.getId(), e2.getId());

        assertThat(delegate.findById(e1.getId()).get().getDlqStatus()).isEqualTo(DlqStatus.IN_PROCESS);
        assertThat(delegate.findById(e2.getId()).get().getDlqStatus()).isEqualTo(DlqStatus.IN_PROCESS);
    }

    @Test
    @DisplayName("IT findAndLockBatchByStatus() should respect batch size via FETCH FIRST")
    void findAndLockBatchByStatus_respectsBatchSizeViaFetchFirst() {
        repository.saveBatch(
                IntStream.range(0, 5)
                        .mapToObj(i -> delegate.buildEvent(DlqStatus.MOVED))
                        .toList()
        );

        assertThat(repository.findAndLockBatchByStatus(DlqStatus.MOVED, 3, DlqStatus.IN_PROCESS))
                .hasSize(3);
    }

    @Test
    @DisplayName("IT findAndLockBatchByStatus() when no matches should return empty")
    void findAndLockBatchByStatus_noMatches_returnsEmpty() {
        assertThat(repository.findAndLockBatchByStatus(DlqStatus.MOVED, 10, DlqStatus.IN_PROCESS))
                .isEmpty();
    }

    @Test
    @DisplayName("IT findAndLockBatchByStatus() should not lock events of other status")
    void findAndLockBatchByStatus_doesNotLockOtherStatuses() {
        OutboxDlqEvent moved    = delegate.buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent resolved = delegate.buildEvent(DlqStatus.RESOLVED);
        repository.saveBatch(List.of(moved, resolved));

        repository.findAndLockBatchByStatus(DlqStatus.MOVED, 10, DlqStatus.IN_PROCESS);

        assertThat(delegate.findById(resolved.getId()).get().getDlqStatus())
                .isEqualTo(DlqStatus.RESOLVED);
    }
}