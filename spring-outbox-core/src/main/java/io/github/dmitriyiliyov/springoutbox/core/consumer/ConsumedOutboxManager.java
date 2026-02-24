package io.github.dmitriyiliyov.springoutbox.core.consumer;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * Manages the state of consumed outbox events to ensure idempotent processing.
 */
public interface ConsumedOutboxManager {

    /**
     * Checks if an event with the given ID has already been consumed.
     *
     * @param id The ID of the event.
     * @return   {@code true} if the event has been consumed, {@code false} otherwise.
     */
    boolean isConsumed(UUID id);

    /**
     * Filters a set of event IDs, returning only those that have not yet been consumed.
     *
     * @param ids The set of event IDs to filter.
     * @return    A set of unconsumed event IDs.
     */
    Set<UUID> filterConsumed(Set<UUID> ids);

    /**
     * Cleans up (deletes) consumed event records that have exceeded their TTL.
     *
     * @param ttl       The duration after which a consumed event record is considered expired.
     * @param batchSize The maximum number of records to clean up in a single operation.
     * @return          The number of cleaned records.
     */
    int cleanBatchByTtl(Duration ttl, int batchSize);
}
