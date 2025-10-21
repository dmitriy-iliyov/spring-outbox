package io.github.dmitriyiliyov.springoutbox.dlq;

import io.github.dmitriyiliyov.springoutbox.core.CacheHelper;
import io.github.dmitriyiliyov.springoutbox.core.OutboxCache;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class DefaultOutboxDlqManager implements OutboxDlqManager {

    private final OutboxDlqRepository repository;
    private final OutboxCache<DlqStatus> cache;

    public DefaultOutboxDlqManager(OutboxDlqRepository repository, OutboxCache<DlqStatus> cache) {
        this.repository = repository;
        this.cache = cache;
    }

    @Override
    public void saveBatch(List<OutboxDlqEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        repository.saveBatch(events);
    }

    @Override
    public long count() {
        return CacheHelper.count(cache, repository::count);
    }

    @Override
    public long countByStatus(DlqStatus status) {
        return CacheHelper.countByStatus(cache, status, repository::countByStatus);
    }

    @Override
    public long countByEventTypeAndStatus(String eventType, DlqStatus status) {
        return CacheHelper.countByEventTypeAndStatus(cache, eventType, status, repository::countByEventTypeAndStatus);
    }

    @Transactional
    @Override
    public List<OutboxDlqEvent> loadBatch(DlqStatus status, int batchSize) {
        List<OutboxDlqEvent> events = repository.findBatchByStatus(status, batchSize);
        if (events.isEmpty()) {
            return events;
        }
        repository.updateBatchStatus(
                events.stream()
                        .map(OutboxEvent::getId)
                        .collect(Collectors.toSet()),
                DlqStatus.IN_PROCESS
        );
        return events;
    }

    @Override
    public List<OutboxDlqEvent> loadBatchByStatus(DlqStatus status, int batchNumber, int batchSize) {
        return repository.findBatchByStatus(status, batchNumber, batchSize);
    }

    @Override
    public void updateStatus(UUID id, DlqStatus status) {
        repository.updateStatus(id, status);
    }

    @Override
    public void updateBatchStatus(Set<UUID> ids, DlqStatus status) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        repository.updateBatchStatus(ids, status);
    }

    @Override
    public void deleteBatch(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        repository.deleteBatch(ids);
    }
}
