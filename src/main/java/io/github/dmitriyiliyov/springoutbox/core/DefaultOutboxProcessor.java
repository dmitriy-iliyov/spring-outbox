package io.github.dmitriyiliyov.springoutbox.core;

import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class DefaultOutboxProcessor implements OutboxProcessor {

    private final OutboxRepository repository;

    public DefaultOutboxProcessor(OutboxRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @Override
    public List<OutboxEvent> loadBatch(String eventType, int batchSize) {
        List<OutboxEvent> events = repository.findBatchByEventTypeAndStatus(eventType, OutboxStatus.PENDING, batchSize);
        repository.updateBatchStatus(
                events.stream()
                        .map(OutboxEvent::getId)
                        .collect(Collectors.toSet()),
                OutboxStatus.IN_PROCESS
        );
        return events;
    }

    @Transactional
    @Override
    public void finalizeBatch(Set<UUID> processedIds, Set<UUID> failedIds, int maxRetryCount) {
        if (processedIds != null && !processedIds.isEmpty()) {
            repository.updateBatchStatus(processedIds, OutboxStatus.PROCESSED);
        }
        if (failedIds != null && !failedIds.isEmpty()) {
            repository.incrementRetryCountOrSetFailed(failedIds, maxRetryCount);
        }
    }

    @Override
    public void cleanUpBatch(Instant threshold, int batchSize) {
        repository.deleteBatchByProcessedAfterThreshold(threshold, batchSize);
    }
}