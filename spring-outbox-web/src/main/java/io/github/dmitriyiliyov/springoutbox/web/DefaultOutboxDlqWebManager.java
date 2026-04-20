package io.github.dmitriyiliyov.springoutbox.web;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.springoutbox.web.exception.OutboxDlqEventBatchNotFoundException;
import io.github.dmitriyiliyov.springoutbox.web.exception.OutboxDlqEventInProcessException;
import io.github.dmitriyiliyov.springoutbox.web.exception.OutboxDlqEventNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class DefaultOutboxDlqWebManager implements OutboxDlqWebManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxDlqWebManager.class);

    private final OutboxDlqWebRepository repository;

    public DefaultOutboxDlqWebManager(OutboxDlqWebRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    @Override
    public OutboxDlqEvent findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new OutboxDlqEventNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @Override
    public List<OutboxDlqEvent> findBatch(BatchRequest request) {
        return repository.findBatchByStatus(request.status(), request.batchNumber(), request.batchSize());
    }

    @Override
    public long count(DlqStatus status) {
        if (status == null) {
            return repository.count();
        }
        return repository.countByStatus(status);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Override
    public void updateStatus(UUID id, DlqStatus status) {
        OutboxDlqEvent event = repository.findById(id).orElseThrow(() -> new OutboxDlqEventNotFoundException(id));
        if (event.getDlqStatus().equals(DlqStatus.IN_PROCESS)) {
            log.debug("Update requested, but event is in 'IN_PROCESS' status; update impossible");
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
            log.debug("Delete requested, but event is in 'IN_PROCESS' status; delete impossible");
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
                log.debug("Some of events is in 'IN_PROCESS' status; operation impossible");
                throw new OutboxDlqEventInProcessException(event.getId());
            }
        }
    }
}
