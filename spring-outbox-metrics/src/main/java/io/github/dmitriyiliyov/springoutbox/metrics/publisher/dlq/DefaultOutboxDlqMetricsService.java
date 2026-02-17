package io.github.dmitriyiliyov.springoutbox.metrics.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.CacheHelper;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.OutboxCache;

public class DefaultOutboxDlqMetricsService implements OutboxDlqMetricsService {

    private final OutboxDlqMetricsRepository repository;
    private final OutboxCache<DlqStatus> cache;

    public DefaultOutboxDlqMetricsService(OutboxDlqMetricsRepository repository, OutboxCache<DlqStatus> cache) {
        this.repository = repository;
        this.cache = cache;
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
        return CacheHelper.countByEventTypeAndStatus(
                cache, eventType, status, repository::countByEventTypeAndStatus
        );
    }
}
