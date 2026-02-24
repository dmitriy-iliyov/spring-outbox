package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.projection.BatchRequestProjection;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.projection.BatchUpdateRequestProjection;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manages the lifecycle of Dead Letter Queue events.
 * <p>
 * This includes saving, loading, and updating the status of events that have failed processing.
 */
public interface OutboxDlqManager {
    /**
     * Saves a batch of DLQ events.
     *
     * @param events The list of DLQ events to save.
     */
    void saveBatch(List<OutboxDlqEvent> events);

    /**
     * Loads a single DLQ event by its ID.
     *
     * @param id The ID of the event to load.
     * @return   The DLQ event, or {@code null} if not found.
     */
    OutboxDlqEvent loadById(UUID id);

    /**
     * Loads and locks a batch of DLQ events with a specific status.
     *
     * @param status    The status of the events to load.
     * @param batchSize The maximum number of events to load.
     * @return          A list of locked DLQ events.
     */
    List<OutboxDlqEvent> loadAndLockBatch(DlqStatus status, int batchSize);

    /**
     * Loads a batch of DLQ events based on a projection request.
     *
     * @param request The projection defining the criteria for loading events.
     * @return        A list of DLQ events.
     */
    List<OutboxDlqEvent> loadBatch(BatchRequestProjection request);

    /**
     * Updates the status of a single DLQ event.
     *
     * @param id     The ID of the event to update.
     * @param status The new status to set.
     */
    void updateStatus(UUID id, DlqStatus status);

    /**
     * Updates the status for a batch of DLQ events based on a projection request.
     *
     * @param request The projection defining the criteria for the batch update.
     */
    void updateBatchStatus(BatchUpdateRequestProjection request);

    /**
     * Deletes a single DLQ event by its ID.
     *
     * @param id The ID of the event to delete.
     * @return   The number of deleted events.
     */
    int deleteById(UUID id);

    /**
     * Deletes a batch of DLQ events by their IDs.
     *
     * @param ids The set of event IDs to delete.
     * @return    The number of deleted events.
     */
    int deleteBatch(Set<UUID> ids);

    /**
     * Deletes a batch of DLQ events with an additional check (e.g., on status).
     *
     * @param ids The set of event IDs to delete.
     * @return    The number of deleted events.
     */
    int deleteBatchWithCheck(Set<UUID> ids);
}
