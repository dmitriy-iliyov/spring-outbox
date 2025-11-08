package io.github.dmitriyiliyov.springoutbox.consumer;

import java.time.Instant;
import java.util.UUID;

public interface OutboxRepository {
    int saveIfAbsent(UUID id);
    void deleteBatchByThreshold(Instant threshold, int batchSize);
}
