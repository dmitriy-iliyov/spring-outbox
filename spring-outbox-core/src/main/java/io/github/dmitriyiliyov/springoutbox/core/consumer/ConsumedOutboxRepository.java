package io.github.dmitriyiliyov.springoutbox.core.consumer;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Defines the data access operations for consumed outbox events.
 * This repository is used to track which events have been successfully processed by a consumer.
 */
public interface ConsumedOutboxRepository {

    /**
     * Saves a single event ID to the repository if it does not already exist.
     * This is a key operation for ensuring idempotency.
     *
     * @param id The UUID of the event to save.
     * @return   The number of rows affected (1 if the ID was inserted, 0 if it already existed).
     */
    int saveIfAbsent(UUID id);

    /**
     * Saves multiple event IDs to the repository if they do not already exist.
     *
     * @param ids A set of UUIDs to save.
     * @return    A set of UUIDs that were successfully inserted (i.e., were not already present).
     */
    Set<UUID> saveIfAbsent(Set<UUID> ids);

    /**
     * Deletes a batch of consumed event records that are older than a given threshold.
     *
     * @param threshold The time threshold for deletion.
     * @param batchSize The maximum number of records to delete in one go.
     * @return          The number of deleted records.
     */
    int deleteBatchByThreshold(Instant threshold, int batchSize);
}
