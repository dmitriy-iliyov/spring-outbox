package io.github.dmitriyiliyov.oncebox.core.consumer;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks consumed outbox events for idempotent processing.
 */
public interface ConsumedOutboxManager {

    /**
     * Attempts to save the given event ID to mark it as consumed.
     * Returns true if the ID was successfully saved (meaning it was not processed before),
     * and false if the ID already exists (meaning it has already been consumed).
     *
     * @param id the ID of the event.
     * @return   {@code true} if the event was successfully marked as consumed, {@code false} if it was already consumed.
     */
    boolean tryConsume(UUID id);

    /**
     * Save only unconsumed events, return already consumed.
     *
     * @param ids the set of event IDs to filter.
     * @return    a subset of already consumed event IDs.
     */
    Set<UUID> tryConsumeAndGetDuplicates(Set<UUID> ids);

    /**
     * Cleans up (deletes) consumed event records that have exceeded their TTL.
     *
     * @param ttl       the duration after which a consumed event record is considered expired.
     * @param batchSize the maximum number of records to delete in a single operation.
     * @return          the number of deleted records.
     */
    int cleanBatchByTtl(Duration ttl, int batchSize);
}
