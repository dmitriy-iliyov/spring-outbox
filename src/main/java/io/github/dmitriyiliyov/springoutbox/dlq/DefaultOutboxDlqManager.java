package io.github.dmitriyiliyov.springoutbox.dlq;

import io.github.dmitriyiliyov.springoutbox.dlq.api.OutboxDlqEventNotFoundException;
import io.github.dmitriyiliyov.springoutbox.dlq.dto.BatchRequest;
import io.github.dmitriyiliyov.springoutbox.dlq.dto.BatchUpdateRequest;
import io.github.dmitriyiliyov.springoutbox.utils.CacheHelper;
import io.github.dmitriyiliyov.springoutbox.utils.OutboxCache;

import java.util.List;
import java.util.Set;
import java.util.UUID;

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

    @Override
    public OutboxDlqEvent loadById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new OutboxDlqEventNotFoundException(id));
    }

    @Override
    public List<OutboxDlqEvent> loadAndLockBatch(DlqStatus status, int batchSize) {
        return repository.findAndLockBatchByStatus(status, batchSize, DlqStatus.IN_PROCESS);
    }

    @Override
    public List<OutboxDlqEvent> loadBatch(BatchRequest request) {
        return repository.findBatchByStatus(request.status(), request.batchNumber(), request.batchSize());
    }

    @Override
    public void updateStatus(UUID id, DlqStatus status) {
        // FIXME IN_PROCESS
        int updatedColumnCount = repository.updateStatus(id, status);
        if (updatedColumnCount == 0) {
            throw new OutboxDlqEventNotFoundException(id);
        }
    }

    @Override
    public void updateBatchStatus(BatchUpdateRequest request) {
        // FIXME IN_PROCESS
        if (request.ids() == null || request.ids().isEmpty()) {
            return;
        }
        repository.updateBatchStatus(request.ids(), request.status());
    }

    @Override
    public void deleteById(UUID id) {
        // FIXME IN_PROCESS
        repository.deleteById(id);
    }

    @Override
    public void deleteBatch(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        // FIXME IN_PROCESS
        repository.deleteBatch(ids);
    }
}
