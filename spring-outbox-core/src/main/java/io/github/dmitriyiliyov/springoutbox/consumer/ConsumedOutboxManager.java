package io.github.dmitriyiliyov.springoutbox.consumer;

import java.time.Duration;
import java.util.UUID;

public interface ConsumedOutboxManager {
    boolean isConsumed(UUID id);
    void cleanBatchByTtl(Duration ttl, int batchSize);
}
