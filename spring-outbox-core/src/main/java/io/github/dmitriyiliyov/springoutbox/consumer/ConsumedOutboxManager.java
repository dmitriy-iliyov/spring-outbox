package io.github.dmitriyiliyov.springoutbox.consumer;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

public interface ConsumedOutboxManager {
    boolean isConsumed(UUID id);

    Set<UUID> filterConsumed(Set<UUID> ids);

    int cleanBatchByTtl(Duration ttl, int batchSize);
}
