package io.github.dmitriyiliyov.springoutbox.dlq.api;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.utils.SqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

class MultiDialectOutboxDlqWebRepositoryIntegrationTests {

    private final OutboxDlqApiRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final SqlIdHelper idHelper;

    MultiDialectOutboxDlqWebRepositoryIntegrationTests(
            OutboxDlqApiRepository repository,
            JdbcTemplate jdbcTemplate, SqlIdHelper idHelper
    ) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.idHelper = idHelper;
    }

    void findById_notExisting_returnsEmpty() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    void findBatch_allIdsExist_returnsAll() {
        OutboxDlqEvent e1 = buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent e2 = buildEvent(DlqStatus.MOVED);
        saveBatch(List.of(e1, e2));

        List<OutboxDlqEvent> result = repository.findBatch(Set.of(e1.getId(), e2.getId()));

        assertThat(result)
                .hasSize(2)
                .extracting(OutboxDlqEvent::getId)
                .containsExactlyInAnyOrder(e1.getId(), e2.getId());
    }

    void findBatch_partiallyExistingIds_returnsOnlyFound() {
        OutboxDlqEvent event = buildEvent(DlqStatus.MOVED);
        saveBatch(List.of(event));

        List<OutboxDlqEvent> result = repository.findBatch(
                Set.of(event.getId(), UUID.randomUUID())
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(event.getId());
    }

    void findBatch_emptyIds_returnsEmptyList() {
        assertThat(repository.findBatch(Set.of())).isEmpty();
    }

    void findBatchByStatus_returnsOnlyMatchingStatus() {
        saveBatch(List.of(
                buildEvent(DlqStatus.MOVED),
                buildEvent(DlqStatus.MOVED),
                buildEvent(DlqStatus.RESOLVED)
        ));

        List<OutboxDlqEvent> result = repository.findBatchByStatus(DlqStatus.MOVED, 0, 10);

        assertThat(result)
                .isNotEmpty()
                .extracting(OutboxDlqEvent::getDlqStatus)
                .containsOnly(DlqStatus.MOVED);
    }

    void findBatchByStatus_pagination_page1AndPage2DoNotOverlap() {
        List<OutboxDlqEvent> events = IntStream.range(0, 5)
                .mapToObj(i -> buildEvent(DlqStatus.MOVED))
                .toList();
        saveBatch(events);

        List<UUID> page1 = repository.findBatchByStatus(DlqStatus.MOVED, 0, 2)
                .stream().map(OutboxDlqEvent::getId).toList();
        List<UUID> page2 = repository.findBatchByStatus(DlqStatus.MOVED, 1, 2)
                .stream().map(OutboxDlqEvent::getId).toList();

        assertThat(page1).hasSize(2);
        assertThat(page2).hasSize(2);
        assertThat(page1).doesNotContainAnyElementsOf(page2);
    }

    void findBatchByStatus_noMatches_returnsEmpty() {
        assertThat(repository.findBatchByStatus(DlqStatus.RESOLVED, 1, 10)).isEmpty();
    }

    void findBatchByStatus_orderedByMovedAt() {
        Instant base = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        OutboxDlqEvent first = buildEventWithMovedAt(DlqStatus.MOVED, base);
        OutboxDlqEvent second = buildEventWithMovedAt(DlqStatus.MOVED, base.plusSeconds(10));
        OutboxDlqEvent third = buildEventWithMovedAt(DlqStatus.MOVED, base.plusSeconds(20));
        saveBatch(List.of(third, first, second));

        List<OutboxDlqEvent> result = repository.findBatchByStatus(DlqStatus.MOVED, 0, 10);

        assertThat(result)
                .extracting(OutboxDlqEvent::getId)
                .containsExactly(first.getId(), second.getId(), third.getId());
    }

    void count_emptyTable_returnsZero() {
        assertThat(repository.count()).isEqualTo(0);
    }

    void count_withEvents_returnsTotalCount() {
        saveBatch(List.of(
                buildEvent(DlqStatus.MOVED),
                buildEvent(DlqStatus.RESOLVED),
                buildEvent(DlqStatus.IN_PROCESS)
        ));

        assertThat(repository.count()).isEqualTo(3);
    }

    void countByStatus_noMatches_returnsZero() {
        saveBatch(List.of(buildEvent(DlqStatus.RESOLVED)));

        assertThat(repository.countByStatus(DlqStatus.MOVED)).isEqualTo(0);
    }

    void countByStatus_withMatches_returnsOnlyMatchingCount() {
        saveBatch(List.of(
                buildEvent(DlqStatus.MOVED),
                buildEvent(DlqStatus.MOVED),
                buildEvent(DlqStatus.RESOLVED)
        ));

        assertThat(repository.countByStatus(DlqStatus.MOVED)).isEqualTo(2);
        assertThat(repository.countByStatus(DlqStatus.RESOLVED)).isEqualTo(1);
        assertThat(repository.countByStatus(DlqStatus.IN_PROCESS)).isEqualTo(0);
    }

    void updateStatus_existingEvent_statusChanged() {
        OutboxDlqEvent event = buildEvent(DlqStatus.MOVED);
        saveBatch(List.of(event));

        repository.updateStatus(event.getId(), DlqStatus.RESOLVED);

        assertThat(repository.findById(event.getId()))
                .isPresent()
                .get()
                .extracting(OutboxDlqEvent::getDlqStatus)
                .isEqualTo(DlqStatus.RESOLVED);
    }

    void updateStatus_notExistingId_doesNotThrow() {
        assertThatCode(() -> repository.updateStatus(UUID.randomUUID(), DlqStatus.RESOLVED))
                .doesNotThrowAnyException();
    }

    void updateBatchStatus_multipleIds_allUpdated() {
        OutboxDlqEvent e1 = buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent e2 = buildEvent(DlqStatus.MOVED);
        saveBatch(List.of(e1, e2));

        repository.updateBatchStatus(Set.of(e1.getId(), e2.getId()), DlqStatus.RESOLVED);

        assertThat(repository.findById(e1.getId()).get().getDlqStatus()).isEqualTo(DlqStatus.RESOLVED);
        assertThat(repository.findById(e2.getId()).get().getDlqStatus()).isEqualTo(DlqStatus.RESOLVED);
    }

    void updateBatchStatus_doesNotAffectOtherEvents() {
        OutboxDlqEvent target = buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent unrelated = buildEvent(DlqStatus.MOVED);
        saveBatch(List.of(target, unrelated));

        repository.updateBatchStatus(Set.of(target.getId()), DlqStatus.RESOLVED);

        assertThat(repository.findById(unrelated.getId()).get().getDlqStatus())
                .isEqualTo(DlqStatus.MOVED);
    }

    void updateBatchStatus_emptyIds_doesNotThrow() {
        assertThatCode(() -> repository.updateBatchStatus(Set.of(), DlqStatus.RESOLVED))
                .doesNotThrowAnyException();
    }

    void deleteById_existingEvent_deletedAndReturnsOne() {
        OutboxDlqEvent event = buildEvent(DlqStatus.MOVED);
        saveBatch(List.of(event));

        int deleted = repository.deleteById(event.getId());

        assertThat(deleted).isEqualTo(1);
        assertThat(repository.findById(event.getId())).isEmpty();
    }

    void deleteById_notExisting_returnsZero() {
        assertThat(repository.deleteById(UUID.randomUUID())).isEqualTo(0);
    }

    void deleteBatch_existingIds_deletedAndReturnsCount() {
        OutboxDlqEvent e1 = buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent e2 = buildEvent(DlqStatus.MOVED);
        saveBatch(List.of(e1, e2));

        int deleted = repository.deleteBatch(Set.of(e1.getId(), e2.getId()));

        assertThat(deleted).isEqualTo(2);
        assertThat(repository.findById(e1.getId())).isEmpty();
        assertThat(repository.findById(e2.getId())).isEmpty();
    }

    void deleteBatch_emptyIds_returnsZero() {
        assertThat(repository.deleteBatch(Set.of())).isEqualTo(0);
    }

    void deleteBatch_doesNotAffectOtherEvents() {
        OutboxDlqEvent target = buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent unrelated = buildEvent(DlqStatus.MOVED);
        saveBatch(List.of(target, unrelated));

        repository.deleteBatch(Set.of(target.getId()));

        assertThat(repository.findById(unrelated.getId())).isPresent();
    }

    protected OutboxDlqEvent buildEvent(DlqStatus dlqStatus) {
        return buildEventWithMovedAt(dlqStatus, Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }

    protected OutboxDlqEvent buildEventWithMovedAt(DlqStatus dlqStatus, Instant movedAt) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return new OutboxDlqEvent(
                UUID.randomUUID(),
                EventStatus.FAILED,
                "ORDER_CREATED",
                "io.example.OrderCreated",
                "{\"orderId\":\"123\"}",
                3,
                now.plusSeconds(60),
                now,
                now,
                dlqStatus,
                movedAt
        );
    }

    public void saveBatch(List<OutboxDlqEvent> eventBatch) {
        String sql = """
            INSERT INTO outbox_dlq_events
            (id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at, moved_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.batchUpdate(
                sql,
                eventBatch,
                eventBatch.size(),
                (ps, event) -> {
                    idHelper.setIdToPs(ps, 1, event.getId());
                    ps.setString(2, event.getStatus().name());
                    ps.setString(3, event.getDlqStatus().name());
                    ps.setString(4, event.getEventType());
                    ps.setString(5, event.getPayloadType());
                    ps.setString(6, event.getPayload());
                    ps.setInt(7, event.getRetryCount());
                    ps.setTimestamp(8, Timestamp.from(event.getNextRetryAt()));
                    ps.setTimestamp(9, Timestamp.from(event.getCreatedAt()));
                    ps.setTimestamp(10, Timestamp.from(event.getUpdatedAt()));
                    ps.setTimestamp(11, Timestamp.from(event.getMovedAt()));
                });
    }
}