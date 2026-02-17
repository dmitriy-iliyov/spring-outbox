package io.github.dmitriyiliyov.springoutbox.metrics.publisher;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;

public interface OutboxMetricsRepository {
    long count();
    long countByStatus(EventStatus status);
    long countByEventTypeAndStatus(String eventType, EventStatus status);
}
