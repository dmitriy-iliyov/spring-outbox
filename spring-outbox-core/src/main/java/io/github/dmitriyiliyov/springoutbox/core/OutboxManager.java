package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.core.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public interface OutboxManager {
    void save(OutboxEvent event);

    void saveBatch(List<OutboxEvent> eventBatch);

    long count();

    long countByStatus(EventStatus status);

    long countByEventTypeAndStatus(String eventType, EventStatus status);

    List<OutboxEvent> loadBatch(String eventType, int batchSize);

    List<OutboxEvent> loadBatch(EventStatus status, int batchSize);

    void finalizeBatch(List<OutboxEvent> events, Set<UUID> processedIds, Set<UUID> failedIds,
                       int maxRetryCount, Function<Integer, Instant> nextRetryAtSupplier);

    void recoverStuckBatch(int batchSize);

    void deleteProcessedBatch(Instant threshold, int batchSize);

    void deleteBatch(Set<UUID> ids);
}
