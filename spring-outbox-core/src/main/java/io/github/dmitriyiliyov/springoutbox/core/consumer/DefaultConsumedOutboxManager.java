package io.github.dmitriyiliyov.springoutbox.core.consumer;

import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    @Override
    public boolean isConsumed(UUID id) {
        return repository.saveIfAbsent(id) == 0;
    }

    @Transactional
    @Override
    public Set<UUID> filterConsumed(Set<UUID> ids) {
        Set<UUID> nonExistedIds = repository.saveIfAbsent(ids);
        return ids.stream()
                .filter(id -> !nonExistedIds.contains(id))
                .collect(Collectors.toSet());
    }

    @Transactional
    @Override
    public int cleanBatchByTtl(Duration ttl, int batchSize) {
        Objects.requireNonNull(ttl, "ttl cannot be null");
        Instant threshold = Instant.now().minusSeconds(ttl.toSeconds());
        return repository.deleteBatchByThreshold(threshold, batchSize);
    }
}
