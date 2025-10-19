package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxStatus;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface OutboxRepository {
    void save(OutboxEvent event);

    void saveBatch(List<OutboxEvent> eventBatch);

    /**
     * Ensures that batches can be safely processed by multiple instances concurrently,
     * so that no event is deleted twice or skipped.
     */
    List<OutboxEvent> findBatchByEventTypeAndStatus(String eventType, OutboxStatus status, int batchSize);

    /**
     * Updates the status of a batch of events.
     *
     * @param ids   the ids of the events to update; must not be null or empty
     * @param status the new status to set; must not be {@link OutboxStatus#FAILED}
     *               (use {@link #incrementRetryCountOrSetFailed(Set, int)} for FAILED)
     */
    void updateBatchStatus(Set<UUID> ids, OutboxStatus status);

    /**
     * Increments the retry count for a batch of events, and sets the status to FAILED
     * if the retry count exceeds the specified maximum.
     *
     * @param ids           the ids of the events to update; must not be null or empty
     * @param maxRetryCount the maximum number of retry attempts before marking an event as FAILED
     */
    void incrementRetryCountOrSetFailed(Set<UUID> ids, int maxRetryCount);

    /**
     * Deletes processed events older than a specified threshold, in batches.
     * Ensures that batches can be safely processed by multiple instances concurrently,
     * so that no event is deleted twice or skipped.
     *
     * @param threshold the cutoff {@link Instant}; only events with {@code processedAt < threshold} will be deleted
     * @param batchSize the maximum number of events to delete in one batch
     */
    void deleteBatchByProcessedAfterThreshold(Instant threshold, int batchSize);
}
