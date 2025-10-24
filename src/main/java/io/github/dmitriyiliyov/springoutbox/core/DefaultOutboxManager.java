package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.core.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.utils.CacheHelper;
import io.github.dmitriyiliyov.springoutbox.utils.OutboxCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class DefaultOutboxManager implements OutboxManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxManager.class);

    private final OutboxRepository repository;
    private final OutboxCache<EventStatus> cache;

    public DefaultOutboxManager(OutboxRepository repository, OutboxCache<EventStatus> cache) {
        this.repository = repository;
        this.cache = cache;
    }

    @Override
    public void save(OutboxEvent event) {
        repository.save(event);
    }

    @Override
    public void saveBatch(List<OutboxEvent> eventBatch) {
        repository.saveBatch(eventBatch);
    }

    @Override
    public long count() {
        return CacheHelper.count(cache, repository::count);
    }

    @Override
    public long countByStatus(EventStatus status) {
        return CacheHelper.countByStatus(cache, status, repository::countByStatus);
    }

    @Override
    public long countByEventTypeAndStatus(String eventType, EventStatus status) {
        return CacheHelper.countByEventTypeAndStatus(cache, eventType, status, repository::countByEventTypeAndStatus);
    }

    @Transactional
    @Override
    public List<OutboxEvent> loadBatch(String eventType, int batchSize) {
        List<OutboxEvent> events = repository.findBatchByEventTypeAndStatus(eventType, EventStatus.PENDING, batchSize);
        if (events.isEmpty()) {
            return events;
        }
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
        if (events.isEmpty()) {
            return events;
        }
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
        if (maxRetryCount < 0) {
            throw new IllegalArgumentException("Parameter maxRetryCount is negative for some reason");
        }

        boolean hasProcessed = !CollectionUtils.isEmpty(processedIds);
        boolean hasFailed = !CollectionUtils.isEmpty(failedIds);

        if (hasProcessed && hasFailed) {
            boolean wasOverlapped = processedIds.removeAll(failedIds);
            if (wasOverlapped) {
                log.warn("Set of ids was overlapped, all overlapped ids deleted from processedIds set");
            }
            if (!processedIds.isEmpty()) {
                repository.updateBatchStatus(processedIds, EventStatus.PROCESSED);
            }
            repository.incrementRetryCountOrSetFailed(failedIds, maxRetryCount);
        } else if (hasProcessed) {
            repository.updateBatchStatus(processedIds, EventStatus.PROCESSED);
        } else if (hasFailed) {
            repository.incrementRetryCountOrSetFailed(failedIds, maxRetryCount);
        } else {
            log.warn("Finalization nullable or empty batch not delegating to repository layer");
        }
    }

    @Override
    public void deleteProcessedBatch(Instant threshold, int batchSize) {
        repository.deleteBatchByProcessedAfterThreshold(threshold, batchSize);
    }

    @Override
    public void deleteBatch(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        repository.deleteBatch(ids);
    }
}
