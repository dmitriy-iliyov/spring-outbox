package io.github.dmitriyiliyov.springoutbox.core.dlq;

import io.github.dmitriyiliyov.springoutbox.core.OutboxCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class DefaultOutboxDlqManager implements OutboxDlqManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxDlqManager.class);
    private final OutboxDlqRepository repository;
    private final OutboxCache<DlqStatus> cache;

    public DefaultOutboxDlqManager(OutboxDlqRepository repository, OutboxCache<DlqStatus> cache) {
        this.repository = repository;
        this.cache = cache;
    }

    @Transactional
    @Override
    public void saveBatch(List<OutboxDlqEvent> events) {
        repository.saveBatch(events);
    }

    @Override
    public List<OutboxDlqEvent> findBatchByStatus(DlqStatus status, int batchNumber, int batchSize) {
        return repository.findBatchByStatus(status, batchNumber, batchSize);
    }

    @Override
    public long count() {
        Long count = cache.getCount();
        if (count != null) {
            return count;
        }
        return cache.putCount(repository.count());
    }

    @Override
    public long countByStatus(DlqStatus status) {
        Long count = cache.getCountByStatus(status);
        if (count != null) {
            return count;
        }
        return cache.putCountByStatus(status, repository.countByStatus(status));
    }

    @Override
    public long countByEventTypeAndStatus(String eventType, DlqStatus status) {
        Long count = cache.getCountByEventTypeAndStatus(eventType, status);
        if (count != null) {
            return count;
        }
        return cache.putCountByEventTypeAndStatus(
                eventType, status,
                repository.countByEventTypeAndStatus(eventType, status)
        );
    }
}
