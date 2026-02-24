package io.github.dmitriyiliyov.springoutbox.metrics.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;

/**
 * A service for retrieving metrics about DLQ events.
 */
public interface OutboxDlqMetricsService {

    /**
     * Counts the total number of DLQ events.
     *
     * @return The total count of DLQ events.
     */
    long count();

    /**
     * Counts the number of DLQ events with a specific status.
     *
     * @param status The status of the DLQ events to count.
     * @return       The count of DLQ events with the given status.
     */
    long countByStatus(DlqStatus status);

    /**
     * Counts the number of DLQ events with a specific event type and status.
     *
     * @param eventType The type of the DLQ events to count.
     * @param status    The status of the DLQ events to count.
     * @return          The count of DLQ events matching the criteria.
     */
    long countByEventTypeAndStatus(String eventType, DlqStatus status);
}
