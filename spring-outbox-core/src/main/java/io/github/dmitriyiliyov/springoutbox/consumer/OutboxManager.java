package io.github.dmitriyiliyov.springoutbox.consumer;

import java.time.Duration;
import java.util.UUID;

public interface OutboxManager {
    boolean saveIfAbsent(UUID id);
    void cleanBatchByTtl(Duration ttl, int batchSize);
}
