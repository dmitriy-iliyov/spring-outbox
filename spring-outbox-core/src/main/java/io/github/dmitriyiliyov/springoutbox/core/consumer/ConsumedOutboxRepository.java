package io.github.dmitriyiliyov.springoutbox.core.consumer;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface ConsumedOutboxRepository {

    /**
     * Saves a single id into the consumed outbox events table if it does not already exist.
     *
     * @param id the UUID to save
     * @return the number of rows affected (1 if the id was inserted, 0 if it already existed)
     */
    int saveIfAbsent(UUID id);

    /**
     * Saves multiple ids into the consumed outbox events if they do not already exist.
     *
     * @param ids a set of UUIDs to save
     * @return a set of UUIDs that inserted into the consumed outbox events table
     */
    Set<UUID> saveIfAbsent(Set<UUID> ids);

    int deleteBatchByThreshold(Instant threshold, int batchSize);
}
