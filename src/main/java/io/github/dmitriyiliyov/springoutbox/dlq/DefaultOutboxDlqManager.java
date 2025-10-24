package io.github.dmitriyiliyov.springoutbox.dlq;

import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import io.github.dmitriyiliyov.springoutbox.dlq.api.OutboxDlqEventNotFoundException;
import io.github.dmitriyiliyov.springoutbox.dlq.dto.BatchRequest;
import io.github.dmitriyiliyov.springoutbox.dlq.dto.BatchUpdateRequest;
import io.github.dmitriyiliyov.springoutbox.utils.CacheHelper;
import io.github.dmitriyiliyov.springoutbox.utils.OutboxCache;
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

    @Override
    public OutboxDlqEvent loadById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new OutboxDlqEventNotFoundException(id));
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
    public List<OutboxDlqEvent> loadBatch(BatchRequest request) {
        return repository.findBatchByStatus(request.status(), request.batchNumber(), request.batchSize());
    }

    @Override
    public void updateStatus(UUID id, DlqStatus status) {
        int updatedColumnCount = repository.updateStatus(id, status);
        if (updatedColumnCount == 0) {
            throw new OutboxDlqEventNotFoundException(id);
        }
    }

    @Override
    public void updateBatchStatus(BatchUpdateRequest request) {
        if (request.ids() == null || request.ids().isEmpty()) {
            return;
        }
        repository.updateBatchStatus(request.ids(), request.status());
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public void deleteBatch(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        repository.deleteBatch(ids);
    }
}
