package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface OutboxProcessor {
    List<OutboxEvent> loadBatch(String eventType, int batchSize);

    void finalizeBatch(Set<UUID> processedIds, Set<UUID> failedIds, int maxRetryCount);

    void cleanUpBatch(Instant threshold, int batchSize);
}
