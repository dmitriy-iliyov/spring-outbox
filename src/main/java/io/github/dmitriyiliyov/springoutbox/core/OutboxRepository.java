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

    List<OutboxEvent> findBatchByEventTypeAndStatus(String eventType, OutboxStatus status, int batchSize);

    void updateBatchStatus(Set<UUID> ids, OutboxStatus status);

    void incrementRetryCountOrSetFailed(Set<UUID> ids, int maxRetryCount);

    void deleteBatchByProcessedAfterThreshold(Instant threshold, int batchSize);
}
