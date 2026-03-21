package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

class AbstractOutboxDlqRepositoryIntegrationTests {

    private final OutboxDlqRepository repository;

    AbstractOutboxDlqRepositoryIntegrationTests(OutboxDlqRepository repository) {
        this.repository = repository;
    }

    void saveBatch_singleEvent_persistedCorrectly() {
        OutboxDlqEvent event = buildEvent(DlqStatus.MOVED);

        repository.saveBatch(List.of(event));

        Optional<OutboxDlqEvent> found = repository.findById(event.getId());
        assertThat(found).isPresent();
        assertThat(found.get()).satisfies(e -> {
            assertThat(e.getId()).isEqualTo(event.getId());
            assertThat(e.getStatus()).isEqualTo(event.getStatus());
            assertThat(e.getDlqStatus()).isEqualTo(event.getDlqStatus());
            assertThat(e.getEventType()).isEqualTo(event.getEventType());
            assertThat(e.getPayloadType()).isEqualTo(event.getPayloadType());
            assertThat(e.getPayload()).isEqualTo(event.getPayload());
            assertThat(e.getRetryCount()).isEqualTo(event.getRetryCount());
        });
    }

    void saveBatch_multipleEvents_allPersisted() {
        List<OutboxDlqEvent> events = List.of(
                buildEvent(DlqStatus.MOVED),
                buildEvent(DlqStatus.RESOLVED),
                buildEvent(DlqStatus.TO_RETRY)
        );

        repository.saveBatch(events);

        events.forEach(e -> assertThat(repository.findById(e.getId())).isPresent());
    }

    void findById_notExisting_returnsEmpty() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    void findBatch_allIdsExist_returnsAll() {
        OutboxDlqEvent e1 = buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent e2 = buildEvent(DlqStatus.MOVED);
        repository.saveBatch(List.of(e1, e2));

        List<OutboxDlqEvent> result = repository.findBatch(Set.of(e1.getId(), e2.getId()));

        assertThat(result)
                .hasSize(2)
                .extracting(OutboxDlqEvent::getId)
                .containsExactlyInAnyOrder(e1.getId(), e2.getId());
    }

    void findBatch_partiallyExistingIds_returnsOnlyFound() {
        OutboxDlqEvent event = buildEvent(DlqStatus.MOVED);
        repository.saveBatch(List.of(event));

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
        repository.saveBatch(List.of(
                buildEvent(DlqStatus.MOVED),
                buildEvent(DlqStatus.MOVED),
                buildEvent(DlqStatus.RESOLVED)
        ));

        List<OutboxDlqEvent> result = repository.findBatchByStatus(DlqStatus.MOVED, 1, 10);

        assertThat(result)
                .isNotEmpty()
                .extracting(OutboxDlqEvent::getDlqStatus)
                .containsOnly(DlqStatus.MOVED);
    }

    void findBatchByStatus_pagination_page1AndPage2DoNotOverlap() {
        List<OutboxDlqEvent> events = IntStream.range(0, 5)
                .mapToObj(i -> buildEvent(DlqStatus.MOVED))
                .toList();
        repository.saveBatch(events);

        List<UUID> page1 = repository.findBatchByStatus(DlqStatus.MOVED, 1, 2)
                .stream().map(OutboxDlqEvent::getId).toList();
        List<UUID> page2 = repository.findBatchByStatus(DlqStatus.MOVED, 2, 2)
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
        repository.saveBatch(List.of(third, first, second));

        List<OutboxDlqEvent> result = repository.findBatchByStatus(DlqStatus.MOVED, 1, 10);

        assertThat(result)
                .extracting(OutboxDlqEvent::getId)
                .containsExactly(first.getId(), second.getId(), third.getId());
    }

    void updateStatus_existingEvent_statusChanged() {
        OutboxDlqEvent event = buildEvent(DlqStatus.MOVED);
        repository.saveBatch(List.of(event));

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
        repository.saveBatch(List.of(e1, e2));

        repository.updateBatchStatus(Set.of(e1.getId(), e2.getId()), DlqStatus.RESOLVED);

        assertThat(repository.findById(e1.getId()).get().getDlqStatus()).isEqualTo(DlqStatus.RESOLVED);
        assertThat(repository.findById(e2.getId()).get().getDlqStatus()).isEqualTo(DlqStatus.RESOLVED);
    }

    void updateBatchStatus_doesNotAffectOtherEvents() {
        OutboxDlqEvent target = buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent unrelated = buildEvent(DlqStatus.MOVED);
        repository.saveBatch(List.of(target, unrelated));

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
        repository.saveBatch(List.of(event));

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
        repository.saveBatch(List.of(e1, e2));

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
        repository.saveBatch(List.of(target, unrelated));

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
}
