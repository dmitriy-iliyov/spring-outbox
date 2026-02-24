package io.github.dmitriyiliyov.springoutbox.metrics.publisher;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;

/**
 * DAO for retrieving outbox event metrics.
 */
public interface OutboxMetricsRepository {

    /**
     * Counts the total number of outbox events in the repository.
     *
     * @return The total count of events.
     */
    long count();

    /**
     * Counts the number of outbox events with a specific status.
     *
     * @param status The status of the events to count.
     * @return       The count of events with the given status.
     */
    long countByStatus(EventStatus status);

    /**
     * Counts the number of outbox events with a specific event type and status.
     *
     * @param eventType The type of the events to count.
     * @param status    The status of the events to count.
     * @return          The count of events matching the criteria.
     */
    long countByEventTypeAndStatus(String eventType, EventStatus status);
}
