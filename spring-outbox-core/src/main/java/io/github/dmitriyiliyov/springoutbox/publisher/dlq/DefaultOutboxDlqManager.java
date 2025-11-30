package io.github.dmitriyiliyov.springoutbox.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.publisher.dlq.dto.BatchRequest;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.dto.BatchUpdateRequest;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.web.exception.OutboxDlqEventBatchNotFoundException;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.web.exception.OutboxDlqEventInProcessException;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.web.exception.OutboxDlqEventNotFoundException;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.CacheHelper;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.OutboxCache;
import org.springframework.transaction.annotation.Isolation;
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

    @Override
    public List<OutboxDlqEvent> loadAndLockBatch(DlqStatus status, int batchSize) {
        return repository.findAndLockBatchByStatus(status, batchSize, DlqStatus.IN_PROCESS);
    }

    @Override
    public List<OutboxDlqEvent> loadBatch(BatchRequest request) {
        return repository.findBatchByStatus(request.status(), request.batchNumber(), request.batchSize());
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Override
    public void updateStatus(UUID id, DlqStatus status) {
        OutboxDlqEvent event = repository.findById(id).orElseThrow(() -> new OutboxDlqEventNotFoundException(id));
        if (event.getDlqStatus().equals(DlqStatus.IN_PROCESS)) {
            throw new OutboxDlqEventInProcessException(event.getId());
        }
        repository.updateStatus(id, status);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Override
    public void updateBatchStatus(BatchUpdateRequest request) {
        if (request.ids() == null || request.ids().isEmpty()) {
            return;
        }
        checkEventsAvailability(request.ids());
        repository.updateBatchStatus(request.ids(), request.status());
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Override
    public int deleteById(UUID id) {
        OutboxDlqEvent event = repository.findById(id).orElseThrow(
                () -> new OutboxDlqEventNotFoundException(id)
        );
        if (event.getDlqStatus().equals(DlqStatus.IN_PROCESS)) {
            throw new OutboxDlqEventInProcessException(event.getId());
        }
        return repository.deleteById(id);
    }

    @Override
    public int deleteBatch(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return repository.deleteBatch(ids);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Override
    public int deleteBatchWithCheck(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        checkEventsAvailability(ids);
        return repository.deleteBatch(ids);
    }

    private void checkEventsAvailability(Set<UUID> ids) {
        List<OutboxDlqEvent> events = repository.findBatch(ids);
        if (events == null || events.isEmpty()) {
            throw new OutboxDlqEventBatchNotFoundException(ids);
        }
        if (events.size() != ids.size()) {
            Set<UUID> foundIds = events.stream()
                    .map(OutboxDlqEvent::getId)
                    .collect(Collectors.toSet());
            ids.removeAll(foundIds);
            throw new OutboxDlqEventBatchNotFoundException(ids);
        }
        for (OutboxDlqEvent event: events) {
            if (event.getDlqStatus().equals(DlqStatus.IN_PROCESS)) {
                throw new OutboxDlqEventInProcessException(event.getId());
            }
        }
    }
}
