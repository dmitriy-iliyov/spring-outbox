package io.github.dmitriyiliyov.springoutbox.consumer;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
        Set<UUID> nonExistedIds = repository.saveIfAbsent(ids);
        return ids.stream()
                .filter(id -> !nonExistedIds.contains(id))
                .collect(Collectors.toSet());
    }

    @Override
    public int cleanBatchByTtl(Duration ttl, int batchSize) {
        Objects.requireNonNull(ttl, "ttl cannot be null");
        Instant threshold = Instant.now().minusSeconds(ttl.toSeconds());
        return repository.deleteBatchByThreshold(threshold, batchSize);
    }
}
