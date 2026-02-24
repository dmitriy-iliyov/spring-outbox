package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * DAO layer for DLQ outbox events.
 * <p>
 * Provides a contract for storing and managing events that have failed processing.
 */
public interface OutboxDlqRepository {

    /**
     * Saves a batch of DLQ events to the repository.
     *
     * @param dlqEvents The list of DLQ events to save.
     */
    void saveBatch(List<OutboxDlqEvent> dlqEvents);

    /**
     * Finds a DLQ event by its ID.
     *
     * @param id The ID of the event.
     * @return   An {@link Optional} containing the event if found, otherwise empty.
     */
    Optional<OutboxDlqEvent> findById(UUID id);

    /**
     * Finds a batch of DLQ events by their IDs.
     *
     * @param ids The set of event IDs to find.
     * @return    A list of found DLQ events.
     */
    List<OutboxDlqEvent> findBatch(Set<UUID> ids);

    /**
     * Finds and locks a batch of DLQ events by their status.
     *
     * @param status     The current status of the events to find.
     * @param batchSize  The maximum number of events to retrieve.
     * @param lockStatus The new status to set for the locked events.
     * @return           A list of locked DLQ events.
     */
    List<OutboxDlqEvent> findAndLockBatchByStatus(DlqStatus status, int batchSize, DlqStatus lockStatus);

    /**
     * Finds a batch of DLQ events by their status with pagination.
     *
     * @param status      The status of the events to find.
     * @param batchNumber The page number (0-based).
     * @param batchSize   The size of the page.
     * @return            A list of DLQ events for the specified page.
     */
    List<OutboxDlqEvent> findBatchByStatus(DlqStatus status, int batchNumber, int batchSize);

    /**
     * Updates the status of a single DLQ event.
     *
     * @param id     The ID of the event to update.
     * @param status The new status to set.
     */
    void updateStatus(UUID id, DlqStatus status);

    /**
     * Updates the status for a batch of DLQ events.
     *
     * @param ids    The IDs of the events to update.
     * @param status The new status to set.
     */
    void updateBatchStatus(Set<UUID> ids, DlqStatus status);

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
}
