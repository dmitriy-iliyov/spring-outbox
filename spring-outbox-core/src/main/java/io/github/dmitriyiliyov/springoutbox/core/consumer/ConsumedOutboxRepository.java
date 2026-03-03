package io.github.dmitriyiliyov.springoutbox.core.consumer;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * DAO layer for consumed outbox events.
 * <p>
 * This repository is responsible for persisting the IDs of processed events to ensure idempotency.
 * It supports atomic "save if absent" operations, which are critical for preventing duplicate processing.
 */
public interface ConsumedOutboxRepository {

    /**
     * Saves a single event ID to the repository if it does not already exist.
     * <p>
     * This operation must be atomic and thread-safe.
     *
     * @param id The UUID of the event to save.
     * @return   The number of rows affected (1 if the ID was inserted, 0 if it already existed).
     */
    int saveIfAbsent(UUID id);

    /**
     * Saves multiple event IDs to the repository if they do not already exist.
     * <p>
     * This is a batch operation for efficiency.
     *
     * @param ids A set of UUIDs to save.
     * @return    A set of UUIDs that were successfully inserted (i.e., were not already present).
     */
    Set<UUID> saveIfAbsent(Set<UUID> ids);

    /**
     * Deletes a batch of consumed event records that are older than a given threshold.
     * <p>
     * Used for cleaning up old records to keep the table size manageable.
     *
     * @param threshold The time threshold (records created before this time will be deleted).
     * @param batchSize The maximum number of records to delete in one operation.
     * @return          The number of deleted records.
     */
    int deleteBatchByThreshold(Instant threshold, int batchSize);
}
