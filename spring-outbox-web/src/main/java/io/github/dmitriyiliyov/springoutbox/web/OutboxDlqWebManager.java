package io.github.dmitriyiliyov.springoutbox.web;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.springoutbox.web.exception.OutboxDlqEventBatchNotFoundException;
import io.github.dmitriyiliyov.springoutbox.web.exception.OutboxDlqEventInProcessException;
import io.github.dmitriyiliyov.springoutbox.web.exception.OutboxDlqEventNotFoundException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface OutboxDlqWebManager {

    /**
     * Loads a single DLQ event by its ID.
     *
     * @param id the ID of the event to load.
     * @return   the DLQ event with the given ID.
     * @throws OutboxDlqEventNotFoundException If no event with the given ID exists.
     */
    OutboxDlqEvent findById(UUID id);

    /**
     * Loads a batch of DLQ events based on a projection request.
     *
     * @param request the projection defining pagination and filtering criteria.
     * @return        list of DLQ events matching the given criteria; empty list if none found.
     */
    List<OutboxDlqEvent> findBatch(BatchRequest request);

    /**
     * Count events by status. If status is null count all events.
     *
     * @param status the status of the event to count.
     */
    long count(DlqStatus status);

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
    void updateBatchStatus(BatchUpdateRequest request);

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
    int deleteBatch(Set<UUID> ids);
}
