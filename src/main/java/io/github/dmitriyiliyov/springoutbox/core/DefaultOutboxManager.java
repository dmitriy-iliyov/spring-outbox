package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.core.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class DefaultOutboxManager implements OutboxManager {

    private final OutboxRepository repository;
    private final OutboxCache<EventStatus> cache;

    public DefaultOutboxManager(OutboxRepository repository, OutboxCache<EventStatus> cache) {
        this.repository = repository;
        this.cache = cache;
    }

    @Transactional
    @Override
    public List<OutboxEvent> loadBatch(String eventType, int batchSize) {
        List<OutboxEvent> events = repository.findBatchByEventTypeAndStatus(eventType, EventStatus.PENDING, batchSize);
        repository.updateBatchStatus(
                events.stream()
                        .map(OutboxEvent::getId)
                        .collect(Collectors.toSet()),
                EventStatus.IN_PROCESS
        );
        return events;
    }

    @Transactional
    @Override
    public List<OutboxEvent> loadBatch(EventStatus status, int batchSize, String orderBy) {
        List<OutboxEvent> events = repository.findBatchByStatus(status, batchSize, orderBy);
        repository.updateBatchStatus(
                events.stream()
                        .map(OutboxEvent::getId)
                        .collect(Collectors.toSet()),
                EventStatus.IN_PROCESS
        );
        return events;
    }

    @Transactional
    @Override
    public void finalizeBatch(Set<UUID> processedIds, Set<UUID> failedIds, int maxRetryCount) {
        if (processedIds != null && !processedIds.isEmpty()) {
            repository.updateBatchStatus(processedIds, EventStatus.PROCESSED);
        }
        if (failedIds != null && !failedIds.isEmpty()) {
            repository.incrementRetryCountOrSetFailed(failedIds, maxRetryCount);
        }
    }

    @Override
    public void deleteBatch(Instant threshold, int batchSize) {
        repository.deleteBatchByProcessedAfterThreshold(threshold, batchSize);
    }

    @Override
    public void deleteBatch(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        repository.deleteBatch(ids);
    }

    @Override
    public long count() {
        Long count = cache.getCount();
        if (count != null) {
            return count;
        }
        return cache.putCount(repository.count());
    }

    @Override
    public long countByStatus(EventStatus status) {
        Long count = cache.getCountByStatus(status);
        if (count != null) {
            return count;
        }
        return cache.putCountByStatus(status, repository.countByStatus(status));
    }

    @Override
    public long countByEventTypeAndStatus(String eventType, EventStatus status) {
        Long count = cache.getCountByEventTypeAndStatus(eventType, status);
        if (count != null) {
            return count;
        }
        return cache.putCountByEventTypeAndStatus(
                eventType, status,
                repository.countByEventTypeAndStatus(eventType, status)
        );
    }
}