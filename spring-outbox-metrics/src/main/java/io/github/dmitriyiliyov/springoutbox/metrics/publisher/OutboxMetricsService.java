package io.github.dmitriyiliyov.springoutbox.metrics.publisher;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;

public interface OutboxMetricsService {
    long count();
    long countByStatus(EventStatus status);
    long countByEventTypeAndStatus(String eventType, EventStatus status);
}
