package io.github.dmitriyiliyov.springoutbox.core.publisher;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * DAO layer for outbox events.
 * <p>
 * Provides a contract for storing, retrieving, and managing events in a persistent store.
 */
public interface OutboxRepository {

    /**
     * Saves a single outbox event.
     *
     * @param event The event to save.
     */
    void save(OutboxEvent event);

    /**
     * Saves a batch of outbox events.
     *
     * @param eventBatch The list of events to save.
     */
    void saveBatch(List<OutboxEvent> eventBatch);

    /**
     * Finds and locks a batch of events by their type and status.
     *
     * @param eventType  The type of events to find.
     * @param status     The current status of events to find.
     * @param batchSize  The maximum number of events to retrieve.
     * @param lockStatus The new status to set for the locked events.
     * @return           A list of locked outbox events.
     */
    List<OutboxEvent> findAndLockBatchByEventTypeAndStatus(String eventType, EventStatus status, int batchSize,
                                                           EventStatus lockStatus);

    /**
     * Finds and locks a batch of events by their status.
     *
     * @param status     The current status of events to find.
     * @param batchSize  The maximum number of events to retrieve.
     * @param lockStatus The new status to set for the locked events.
     * @return           A list of locked outbox events.
     */
    List<OutboxEvent> findAndLockBatchByStatus(EventStatus status, int batchSize, EventStatus lockStatus);

    /**
     * Updates the status for a batch of events.
     *
     * @param ids       The IDs of the events to update.
     * @param newStatus The new status to set.
     * @return          The number of updated events.
     */
    int updateBatchStatus(Set<UUID> ids, EventStatus newStatus);

    /**
     * Updates the status for a batch of events that match a status and are older than a threshold.
     *
     * @param status     The current status of events to update.
     * @param threshold  The time threshold.
     * @param batchSize  The maximum number of events to update.
     * @param newStatus  The new status to set.
     * @return           The number of updated events.
     */
    int updateBatchStatusByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize, EventStatus newStatus);

    /**
     * Partially updates a batch of events (increments retry count, sets next retry time).
     *
     * @param events The list of events to update.
     * @return       The number of updated events.
     */
    int partiallyUpdateBatch(List<OutboxEvent> events);

    /**
     * Deletes a batch of events by their IDs.
     *
     * @param ids The set of event IDs to delete.
     * @return    The number of deleted events.
     */
    int deleteBatch(Set<UUID> ids);

    /**
     * Deletes a batch of events that match a status and are older than a threshold.
     *
     * @param status    The status of events to delete.
     * @param threshold The time threshold.
     * @param batchSize The maximum number of events to delete.
     * @return          The number of deleted events.
     */
    int deleteBatchByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize);
}
