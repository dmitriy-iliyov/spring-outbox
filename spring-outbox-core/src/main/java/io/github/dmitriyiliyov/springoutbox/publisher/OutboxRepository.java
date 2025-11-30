package io.github.dmitriyiliyov.springoutbox.publisher;

import io.github.dmitriyiliyov.springoutbox.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxEvent;

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

    int updateBatchStatus(Set<UUID> ids, EventStatus newStatus);

    int updateBatchStatusByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize, EventStatus newStatus);

    int partiallyUpdateBatch(List<OutboxEvent> events);

    int deleteBatch(Set<UUID> ids);

    int deleteBatchByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize);
}
