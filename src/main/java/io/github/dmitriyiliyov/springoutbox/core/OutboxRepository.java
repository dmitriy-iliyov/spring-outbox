package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.core.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface OutboxRepository {
    void save(OutboxEvent event);

    void saveBatch(List<OutboxEvent> eventBatch);

    long count();

    long countByStatus(EventStatus status);

    long countByEventTypeAndStatus(String eventType, EventStatus status);

    List<OutboxEvent> findAndLockBatchByEventTypeAndStatus(String eventType, EventStatus status, int batchSize,
                                                           EventStatus lockStatus);

    List<OutboxEvent> findAndLockBatchByStatus(EventStatus status, int batchSize, EventStatus lockStatus);

    void updateBatchStatus(Set<UUID> ids, EventStatus newStatus);

    int updateBatchStatusByStatus(EventStatus status, int batchSize, EventStatus newStatus);

    void partiallyUpdateBatch(List<OutboxEvent> events);

    void deleteBatch(Set<UUID> ids);

    void deleteBatchByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize);
}
