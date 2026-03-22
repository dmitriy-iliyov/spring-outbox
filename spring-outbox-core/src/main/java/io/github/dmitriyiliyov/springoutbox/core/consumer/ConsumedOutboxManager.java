package io.github.dmitriyiliyov.springoutbox.core.consumer;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks consumed outbox events for idempotent processing.
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
     * Filters out already consumed IDs, returning only unconsumed ones.
     * The name refers to the filtering operation applied to consumed records,
     * not to the content of the returned set.
     *
     * @param ids the set of event IDs to filter.
     * @return    a subset of unconsumed event IDs.
     */
    Set<UUID> filterConsumed(Set<UUID> ids);

    /**
     * Cleans up (deletes) consumed event records that have exceeded their TTL.
     *
     * @param ttl       the duration after which a consumed event record is considered expired.
     * @param batchSize the maximum number of records to delete in a single operation.
     * @return          the number of deleted records.
     */
    int cleanBatchByTtl(Duration ttl, int batchSize);
}
