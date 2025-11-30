package io.github.dmitriyiliyov.springoutbox.publisher;

import io.github.dmitriyiliyov.springoutbox.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.CacheHelper;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.OutboxCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class DefaultOutboxManager implements OutboxManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxManager.class);

    protected final OutboxRepository repository;
    protected final OutboxCache<EventStatus> cache;

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

    @Override
    public List<OutboxEvent> loadBatch(String eventType, int batchSize) {
        return repository.findAndLockBatchByEventTypeAndStatus(
                eventType,
                EventStatus.PENDING,
                batchSize,
                EventStatus.IN_PROCESS
        );
    }

    @Override
    public List<OutboxEvent> loadBatch(EventStatus status, int batchSize) {
        return repository.findAndLockBatchByStatus(status, batchSize, EventStatus.IN_PROCESS);
    }

    @Transactional
    @Override
    public void finalizeBatch(List<OutboxEvent> events, Set<UUID> processedIds, Set<UUID> failedIds,
                              int maxRetryCount, Function<Integer, Instant> nextRetryAtSupplier) {
        if (maxRetryCount < 0) {
            throw new IllegalArgumentException("Parameter maxRetryCount is negative for some reason");
        }

        boolean hasProcessed = !CollectionUtils.isEmpty(processedIds);
        boolean hasFailed = !CollectionUtils.isEmpty(failedIds);

        if (hasProcessed && hasFailed) {
            boolean wasOverlapped = processedIds.removeAll(failedIds);
            if (wasOverlapped) {
                log.warn("Set of ids was overlapped, all overlapped ids deleted moved from processedIds set to failedIds");
            }
            if (!processedIds.isEmpty()) {
                repository.updateBatchStatus(processedIds, EventStatus.PROCESSED);
            }
            repository.partiallyUpdateBatch(prepareFailedEvents(events, failedIds, maxRetryCount, nextRetryAtSupplier));
        } else if (hasProcessed) {
            repository.updateBatchStatus(processedIds, EventStatus.PROCESSED);
        } else if (hasFailed) {
            repository.partiallyUpdateBatch(prepareFailedEvents(events, failedIds, maxRetryCount, nextRetryAtSupplier));
        } else {
            log.warn("Finalization nullable or empty batch not delegating to repository layer");
        }
    }

    private List<OutboxEvent> prepareFailedEvents(List<OutboxEvent> events, Set<UUID> failedIds,
                                                  int maxRetryCount, Function<Integer, Instant> nextRetryAtSupplier) {
        return events.stream()
                .filter(event -> failedIds.contains(event.getId()))
                .map(event -> {
                            EventStatus newStatus;
                            int newRetryCount = event.getRetryCount() + 1;                     // because this is after try
                            Instant nextRetryAt;
                            if (newRetryCount < maxRetryCount) {
                                newStatus = EventStatus.PENDING;
                                nextRetryAt = nextRetryAtSupplier.apply(newRetryCount + 1); // should count time for next try
                            } else {
                                newStatus = EventStatus.FAILED;
                                nextRetryAt = event.getNextRetryAt();
                            }
                            return new OutboxEvent(
                                    event.getId(),
                                    newStatus,
                                    event.getEventType(),
                                    event.getPayloadType(),
                                    event.getPayload(),
                                    Math.min(newRetryCount, maxRetryCount),
                                    nextRetryAt,
                                    event.getCreatedAt(),
                                    Instant.now()
                            );
                })
                .toList();
    }

    @Override
    public int recoverStuckBatch(Duration maxBatchProcessingTime, int batchSize) {
        int recoverSize = repository.updateBatchStatusByStatusAndThreshold(
                EventStatus.IN_PROCESS,
                Instant.now().minusSeconds(maxBatchProcessingTime.toSeconds()),
                batchSize,
                EventStatus.PENDING
        );
        if (recoverSize > 0) {
            log.warn("Stuck events batch recovered, recoveredSize={}; batchSize={} ", recoverSize, batchSize);
        }
        return recoverSize;
    }

    @Override
    public int deleteProcessedBatch(Instant threshold, int batchSize) {
        return repository.deleteBatchByStatusAndThreshold(EventStatus.PROCESSED, threshold, batchSize);
    }

    @Override
    public int deleteBatch(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return repository.deleteBatch(ids);
    }
}
