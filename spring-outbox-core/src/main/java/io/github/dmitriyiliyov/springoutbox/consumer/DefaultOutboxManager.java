package io.github.dmitriyiliyov.springoutbox.consumer;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class DefaultOutboxManager implements OutboxManager {

    private static final String CACHE_NAME = "outbox-consumed-events";

    private final CacheManager cacheManager;
    private final OutboxRepository repository;

    public DefaultOutboxManager(CacheManager cacheManager, OutboxRepository repository) {
        this.cacheManager = cacheManager;
        this.repository = repository;
    }

    @Override
    public boolean saveIfAbsent(UUID id) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        Boolean isExists;
        if (cache != null) {
            isExists = cache.get(id.toString(), Boolean.class);
        } else {
            throw new IllegalStateException("Cache for outbox with name %s not found".formatted(CACHE_NAME));
        }
        if (isExists == null) {
            isExists = repository.saveIfAbsent(id) == 1;
            cache.put(id.toString(), true);
        }
        return isExists;
    }

    @Override
    public void cleanBatchByTtl(Duration ttl, int batchSize) {
        Objects.requireNonNull(ttl, "ttl cannot be null");
        Instant threshold = Instant.now().minusSeconds(ttl.toSeconds());
        repository.deleteBatchByThreshold(threshold, batchSize);
    }
}
