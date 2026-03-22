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
     * <p>
     * Does nothing if the list is null or empty.
     *
     * @param dlqEvents the list of DLQ events to save.
     */
    void saveBatch(List<OutboxDlqEvent> dlqEvents);

    /**
     * Finds a DLQ event by its ID.
     *
     * @param id the ID of the event.
     * @return   an {@link Optional} containing the event if found, otherwise empty.
     */
    Optional<OutboxDlqEvent> findById(UUID id);

    /**
     * Finds a batch of DLQ events by their IDs.
     * <p>
     * Returns an empty list if the set is null or empty.
     * Only events that actually exist are returned — no exception is thrown for missing IDs.
     *
     * @param ids the set of event IDs to find.
     * @return    a list of found DLQ events; may be smaller than {@code ids} if some do not exist.
     */
    List<OutboxDlqEvent> findBatch(Set<UUID> ids);

    /**
     * Finds and locks a batch of DLQ events by their status.
     * <p>
     * Expected to use row-level locking (e.g. {@code SELECT ... FOR UPDATE SKIP LOCKED}).
     * May return fewer than {@code batchSize} events under contention.
     * Implementations should atomically update the status to {@code lockStatus}.
     *
     * @param status     the current status of the events to find.
     * @param batchSize  the maximum number of events to retrieve.
     * @param lockStatus the new status to set for the locked events.
     * @return           a list of locked DLQ events with status set to {@code lockStatus}.
     */
    List<OutboxDlqEvent> findAndLockBatchByStatus(DlqStatus status, int batchSize, DlqStatus lockStatus);

    /**
     * Finds a batch of DLQ events by their status with pagination.
     * <p>
     * Results are ordered by {@code moved_at} ascending.
     *
     * @param status      the status of the events to find.
     * @param batchNumber the page number (1-based).
     * @param batchSize   the number of events per page.
     * @return            a list of DLQ events for the specified page; empty list if none found.
     */
    List<OutboxDlqEvent> findBatchByStatus(DlqStatus status, int batchNumber, int batchSize);

    /**
     * Updates the status of a single DLQ event.
     * <p>
     * If no event with the given ID exists, the operation is a no-op.
     *
     * @param id     the ID of the event to update.
     * @param status the new status to set.
     */
    void updateStatus(UUID id, DlqStatus status);

    /**
     * Updates the status for a batch of DLQ events.
     * <p>
     * Does nothing if the set is null or empty.
     * IDs that do not correspond to existing events are silently ignored.
     *
     * @param ids    the IDs of the events to update.
     * @param status the new status to set.
     */
    void updateBatchStatus(Set<UUID> ids, DlqStatus status);

    /**
     * Deletes a single DLQ event by its ID.
     *
     * @param id the ID of the event to delete.
     * @return   1 if the event was deleted, 0 if no event with the given ID exists.
     */
    int deleteById(UUID id);

    /**
     * Deletes a batch of DLQ events by their IDs.
     * <p>
     * Does nothing and returns 0 if the set is null or empty.
     * IDs that do not correspond to existing events are silently ignored.
     *
     * @param ids the set of event IDs to delete.
     * @return    the number of actually deleted events.
     */
    int deleteBatch(Set<UUID> ids);
}