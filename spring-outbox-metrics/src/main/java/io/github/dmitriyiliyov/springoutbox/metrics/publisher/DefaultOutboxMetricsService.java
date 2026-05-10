package io.github.dmitriyiliyov.springoutbox.metrics.publisher;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.CacheHelper;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.OutboxCache;

import java.util.Objects;

public class DefaultOutboxMetricsService implements OutboxMetricsService {

    private final OutboxMetricsRepository repository;
    private final OutboxCache<EventStatus> cache;

    public DefaultOutboxMetricsService(OutboxMetricsRepository repository, OutboxCache<EventStatus> cache) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.cache = Objects.requireNonNull(cache, "cache cannot be null");
    }

    @Override
    public long count() {
        return CacheHelper.count(cache, repository::count);
    }

    @Override
    public long countByStatus(EventStatus status) {
        return CacheHelper.countByStatus(cache, status, repository::countByStatus);
    }

    @Override
    public long countByEventTypeAndStatus(String eventType, EventStatus status) {
        return CacheHelper.countByEventTypeAndStatus(cache, eventType, status, repository::countByEventTypeAndStatus);
    }
}
