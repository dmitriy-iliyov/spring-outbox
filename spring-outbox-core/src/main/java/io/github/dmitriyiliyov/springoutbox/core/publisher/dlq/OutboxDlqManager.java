package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.exception.OutboxDlqEventBatchNotFoundException;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.exception.OutboxDlqEventInProcessException;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.exception.OutboxDlqEventNotFoundException;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.projection.BatchRequestProjection;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.projection.BatchUpdateRequestProjection;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manages the lifecycle of DLQ events.
 * <p>
 * Provides operations for saving, loading, updating, and deleting events
 * that have failed processing and been moved to the DLQ.
 */
public interface OutboxDlqManager {

    /**
     * Saves a batch of DLQ events.
     * <p>
     * Does nothing if the list is null or empty.
     *
     * @param events the list of DLQ events to save.
     */
    void saveBatch(List<OutboxDlqEvent> events);

    /**
     * Loads a single DLQ event by its ID.
     *
     * @param id the ID of the event to load.
     * @return   the DLQ event with the given ID.
     * @throws OutboxDlqEventNotFoundException If no event with the given ID exists.
     */
    OutboxDlqEvent loadById(UUID id);

    /**
     * Loads and locks a batch of DLQ events with a specific status.
     * <p>
     * Locked events have their status changed to {@link DlqStatus#IN_PROCESS},
     * preventing concurrent processing by other consumers.
     *
     * @param status    the current status of the events to load.
     * @param batchSize the maximum number of events to load and lock.
     * @return          list of locked DLQ events with status set to {@link DlqStatus#IN_PROCESS}.
     */
    List<OutboxDlqEvent> loadAndLockBatch(DlqStatus status, int batchSize);

    /**
     * Loads a batch of DLQ events based on a projection request.
     *
     * @param request the projection defining pagination and filtering criteria.
     * @return        list of DLQ events matching the given criteria; empty list if none found.
     */
    List<OutboxDlqEvent> loadBatch(BatchRequestProjection request);

    /**
     * Updates the status of a single DLQ event.
     *
     * @param id     the ID of the event to update.
     * @param status the new status to set.
     * @throws OutboxDlqEventNotFoundException  if no event with the given ID exists.
     * @throws OutboxDlqEventInProcessException if the event currently has status {@link DlqStatus#IN_PROCESS}.
     */
    void updateStatus(UUID id, DlqStatus status);

    /**
     * Updates the status for a batch of DLQ events based on a projection request.
     * <p>
     * Does nothing if the request contains a null or empty set of IDs.
     *
     * @param request the projection containing the IDs and the new status to set.
     * @throws OutboxDlqEventBatchNotFoundException if any of the provided IDs do not exist.
     * @throws OutboxDlqEventInProcessException     if any of the events has status {@link DlqStatus#IN_PROCESS}.
     */
    void updateBatchStatus(BatchUpdateRequestProjection request);

    /**
     * Deletes a single DLQ event by its ID.
     *
     * @param id the ID of the event to delete.
     * @return   the number of deleted events (1 if deleted, 0 if not found).
     * @throws OutboxDlqEventNotFoundException  if no event with the given ID exists.
     * @throws OutboxDlqEventInProcessException if the event currently has status {@link DlqStatus#IN_PROCESS}.
     */
    int deleteById(UUID id);

    /**
     * Deletes a batch of DLQ events by their IDs without any additional checks.
     * <p>
     * Does nothing and returns 0 if the set is null or empty.
     *
     * @param ids the set of event IDs to delete.
     * @return    the number of deleted events.
     */
    int deleteBatch(Set<UUID> ids);

    /**
     * Deletes a batch of DLQ events after verifying that all events exist
     * and none has status {@link DlqStatus#IN_PROCESS}.
     * <p>
     * Does nothing and returns 0 if the set is null or empty.
     *
     * @param ids the set of event IDs to delete.
     * @return    the number of deleted events.
     * @throws OutboxDlqEventBatchNotFoundException if any of the provided IDs do not exist.
     * @throws OutboxDlqEventInProcessException     if any of the events has status {@link DlqStatus#IN_PROCESS}.
     */
    int deleteBatchWithCheck(Set<UUID> ids);
}