package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.exception.OutboxDlqEventBatchNotFoundException;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.exception.OutboxDlqEventInProcessException;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.exception.OutboxDlqEventNotFoundException;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.projection.BatchRequestProjection;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.projection.BatchUpdateRequestProjection;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class DefaultOutboxDlqManager implements OutboxDlqManager {

    private final OutboxDlqRepository repository;

    public DefaultOutboxDlqManager(OutboxDlqRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @Override
    public void saveBatch(List<OutboxDlqEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        repository.saveBatch(events);
    }

    @Transactional(readOnly = true)
    @Override
    public OutboxDlqEvent loadById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new OutboxDlqEventNotFoundException(id));
    }

    @Transactional
    @Override
    public List<OutboxDlqEvent> loadAndLockBatch(DlqStatus status, int batchSize) {
        return repository.findAndLockBatchByStatus(status, batchSize, DlqStatus.IN_PROCESS);
    }

    @Transactional(readOnly = true)
    @Override
    public List<OutboxDlqEvent> loadBatch(BatchRequestProjection request) {
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
    public void updateBatchStatus(BatchUpdateRequestProjection request) {
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

    @Transactional
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
