package io.github.dmitriyiliyov.springoutbox.web;

import io.github.dmitriyiliyov.springoutbox.core.utils.MySqlIdHelper;
import io.github.dmitriyiliyov.springoutbox.web.it.BaseMySqlIntegrationTests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MySqlMultiDialectOutboxDlqWebRepositoryIntegrationTests extends BaseMySqlIntegrationTests {

    private final MultiDialectOutboxDlqWebRepositoryIntegrationTests delegate;

    public MySqlMultiDialectOutboxDlqWebRepositoryIntegrationTests(
            @Qualifier("mysqlOutboxDlqWebRepository") OutboxDlqWebRepository repository,
            @Qualifier("mysqlJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        this.delegate = new MultiDialectOutboxDlqWebRepositoryIntegrationTests(
                repository,
                jdbcTemplate,
                new MySqlIdHelper()
        );
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
    @DisplayName("IT count() when table is empty should return 0")
    void count_emptyTable_returnsZero() {
        delegate.count_emptyTable_returnsZero();
    }

    @Test
    @DisplayName("IT count() when table has events should return total count")
    void count_withEvents_returnsTotalCount() {
        delegate.count_withEvents_returnsTotalCount();
    }

    @Test
    @DisplayName("IT countByStatus() when no matches should return 0")
    void countByStatus_noMatches_returnsZero() {
        delegate.countByStatus_noMatches_returnsZero();
    }

    @Test
    @DisplayName("IT countByStatus() when matches exist should return only matching count")
    void countByStatus_withMatches_returnsOnlyMatchingCount() {
        delegate.countByStatus_withMatches_returnsOnlyMatchingCount();
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
}