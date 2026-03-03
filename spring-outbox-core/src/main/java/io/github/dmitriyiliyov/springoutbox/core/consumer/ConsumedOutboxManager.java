package io.github.dmitriyiliyov.springoutbox.core.consumer;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * Manages the state of consumed outbox events to ensure idempotent processing.
 * <p>
 * This interface provides methods to check if an event has already been processed and to clean up old records.
 */
public interface ConsumedOutboxManager {

    /**
     * Checks if an event with the given ID has already been consumed.
     * <p>
     * This method is used for single-message processing.
     *
     * @param id The ID of the event.
     * @return   {@code true} if the event has been consumed, {@code false} otherwise.
     */
    boolean isConsumed(UUID id);

    /**
     * Filters a set of event IDs, returning only those that have NOT yet been consumed.
     * <p>
     * This method is used for batch processing to skip already processed messages.
     *
     * @param ids The set of event IDs to filter.
     * @return    A subset of unconsumed event IDs.
     */
    Set<UUID> filterConsumed(Set<UUID> ids);

    /**
     * Cleans up (deletes) consumed event records that have exceeded their TTL.
     * <p>
     * This is a maintenance operation to prevent the consumed events table from growing indefinitely.
     *
     * @param ttl       The duration after which a consumed event record is considered expired.
     * @param batchSize The maximum number of records to delete in a single operation.
     * @return          The number of deleted records.
     */
    int cleanBatchByTtl(Duration ttl, int batchSize);
}
