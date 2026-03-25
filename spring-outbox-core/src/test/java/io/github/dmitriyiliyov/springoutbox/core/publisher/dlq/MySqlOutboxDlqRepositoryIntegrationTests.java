package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.it.BaseMySqlIntegrationTests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class MySqlOutboxDlqRepositoryIntegrationTests extends BaseMySqlIntegrationTests {

    private final OutboxDlqRepository repository;
    private final AbstractOutboxDlqRepositoryIntegrationTests delegate;

    public MySqlOutboxDlqRepositoryIntegrationTests(@Qualifier("mysqlOutboxDlqRepository") OutboxDlqRepository repository) {
        this.repository = repository;
        this.delegate = new AbstractOutboxDlqRepositoryIntegrationTests(repository);
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
    @DisplayName("IT findById() for not existing id should return empty")
    void findById_notExisting_returnsEmpty() {
        delegate.findById_notExisting_returnsEmpty();
    }

    @Test
    @DisplayName("IT findBatch() when all ids exist should return all")
    void findBatch_allIdsExist_returnsAll() {
        delegate.findBatch_allIdsExist_returnsAll();
    }

    @Test
    @DisplayName("IT findBatch() when partially existing ids should return only found")
    void findBatch_partiallyExistingIds_returnsOnlyFound() {
        delegate.findBatch_partiallyExistingIds_returnsOnlyFound();
    }

    @Test
    @DisplayName("IT findBatch() when ids is empty should return empty list")
    void findBatch_emptyIds_returnsEmptyList() {
        delegate.findBatch_emptyIds_returnsEmptyList();
    }

    @Test
    @DisplayName("IT findBatchByStatus() should return only matching status")
    void findBatchByStatus_returnsOnlyMatchingStatus() {
        delegate.findBatchByStatus_returnsOnlyMatchingStatus();
    }

    @Test
    @DisplayName("IT findBatchByStatus() with pagination should not overlap pages")
    void findBatchByStatus_pagination_page1AndPage2DoNotOverlap() {
        delegate.findBatchByStatus_pagination_page1AndPage2DoNotOverlap();
    }

    @Test
    @DisplayName("IT findBatchByStatus() when no matches should return empty")
    void findBatchByStatus_noMatches_returnsEmpty() {
        delegate.findBatchByStatus_noMatches_returnsEmpty();
    }

    @Test
    @DisplayName("IT findBatchByStatus() should be ordered by movedAt")
    void findBatchByStatus_orderedByMovedAt() {
        delegate.findBatchByStatus_orderedByMovedAt();
    }

    @Test
    @DisplayName("IT updateStatus() for existing event should change status")
    void updateStatus_existingEvent_statusChanged() {
        delegate.updateStatus_existingEvent_statusChanged();
    }

    @Test
    @DisplayName("IT updateStatus() for not existing id should not throw")
    void updateStatus_notExistingId_doesNotThrow() {
        delegate.updateStatus_notExistingId_doesNotThrow();
    }

    @Test
    @DisplayName("IT updateBatchStatus() for multiple ids should update all")
    void updateBatchStatus_multipleIds_allUpdated() {
        delegate.updateBatchStatus_multipleIds_allUpdated();
    }

    @Test
    @DisplayName("IT updateBatchStatus() should not affect other events")
    void updateBatchStatus_doesNotAffectOtherEvents() {
        delegate.updateBatchStatus_doesNotAffectOtherEvents();
    }

    @Test
    @DisplayName("IT updateBatchStatus() with empty ids should not throw")
    void updateBatchStatus_emptyIds_doesNotThrow() {
        delegate.updateBatchStatus_emptyIds_doesNotThrow();
    }

    @Test
    @DisplayName("IT deleteById() for existing event should delete and return one")
    void deleteById_existingEvent_deletedAndReturnsOne() {
        delegate.deleteById_existingEvent_deletedAndReturnsOne();
    }

    @Test
    @DisplayName("IT deleteById() for not existing id should return zero")
    void deleteById_notExisting_returnsZero() {
        delegate.deleteById_notExisting_returnsZero();
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

        assertThat(repository.findById(e1.getId()).get().getDlqStatus()).isEqualTo(DlqStatus.IN_PROCESS);
        assertThat(repository.findById(e2.getId()).get().getDlqStatus()).isEqualTo(DlqStatus.IN_PROCESS);
    }

    @Test
    @DisplayName("IT findAndLockBatchByStatus() should respect batch size")
    void findAndLockBatchByStatus_respectsBatchSize() {
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
        List<OutboxDlqEvent> locked = repository
                .findAndLockBatchByStatus(DlqStatus.MOVED, 10, DlqStatus.IN_PROCESS);

        assertThat(locked).isEmpty();
    }

    @Test
    @DisplayName("IT findAndLockBatchByStatus() should not lock events of other status")
    void findAndLockBatchByStatus_doesNotLockOtherStatuses() {
        OutboxDlqEvent moved    = delegate.buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent resolved = delegate.buildEvent(DlqStatus.RESOLVED);
        repository.saveBatch(List.of(moved, resolved));

        repository.findAndLockBatchByStatus(DlqStatus.MOVED, 10, DlqStatus.IN_PROCESS);

        assertThat(repository.findById(resolved.getId()).get().getDlqStatus())
                .isEqualTo(DlqStatus.RESOLVED);
    }
}