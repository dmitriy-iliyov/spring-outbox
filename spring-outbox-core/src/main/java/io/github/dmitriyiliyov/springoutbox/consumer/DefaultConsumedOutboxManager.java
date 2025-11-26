package io.github.dmitriyiliyov.springoutbox.consumer;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class DefaultConsumedOutboxManager implements ConsumedOutboxManager {

    protected final ConsumedOutboxRepository repository;

    public DefaultConsumedOutboxManager(ConsumedOutboxRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isConsumed(UUID id) {
        return repository.saveIfAbsent(id) == 0;
    }

    @Override
    public Set<UUID> filterConsumed(Set<UUID> ids) {
        return repository.saveIfAbsent(ids);
    }

    @Override
    public void cleanBatchByTtl(Duration ttl, int batchSize) {
        Objects.requireNonNull(ttl, "ttl cannot be null");
        Instant threshold = Instant.now().minusSeconds(ttl.toSeconds());
        repository.deleteBatchByThreshold(threshold, batchSize);
    }
}
