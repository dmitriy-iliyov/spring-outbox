package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DefaultOutboxDlqManager implements OutboxDlqManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxDlqManager.class);

    private final OutboxDlqRepository repository;

    public DefaultOutboxDlqManager(OutboxDlqRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @Override
    public void saveBatch(List<OutboxDlqEvent> events) {
        if (events.isEmpty()) {
            log.debug("List of submitted events is empty, so saving was rejected");
            return;
        }
        repository.saveBatch(events);
    }

    @Transactional
    @Override
    public List<OutboxDlqEvent> loadAndLockBatch(DlqStatus status, int batchSize) {
        return repository.findAndLockBatchByStatus(status, batchSize, DlqStatus.IN_PROCESS);
    }

    @Transactional
    @Override
    public int deleteBatch(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            log.debug("List of submitted ids is empty, so deleting was rejected");
            return 0;
        }
        return repository.deleteBatch(ids);
    }
}
