package io.github.dmitriyiliyov.oncebox.metrics.publisher.dlq;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.metrics.publisher.utils.CacheHelper;
import io.github.dmitriyiliyov.oncebox.metrics.publisher.utils.OutboxCache;

import java.util.Objects;

public class DefaultOutboxDlqMetricsService implements OutboxDlqMetricsService {

    private final OutboxDlqMetricsRepository repository;
    private final OutboxCache<DlqStatus> cache;

    public DefaultOutboxDlqMetricsService(OutboxDlqMetricsRepository repository, OutboxCache<DlqStatus> cache) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.cache = Objects.requireNonNull(cache, "cache cannot be null");
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
