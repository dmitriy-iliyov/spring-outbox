package io.github.dmitriyiliyov.springoutbox.metrics.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;

public interface OutboxDlqMetricsService {
    long count();
    long countByStatus(DlqStatus status);
    long countByEventTypeAndStatus(String eventType, DlqStatus status);
}
