package io.github.dmitriyiliyov.springoutbox.core.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class ConsumedOutboxManagerCacheDecorator implements ConsumedOutboxManager {

    private static final Logger log = LoggerFactory.getLogger(ConsumedOutboxManagerCacheDecorator.class);

    private final String cacheName;
    private final CacheManager cacheManager;
    private final ConsumedOutboxManager delegate;
    private final ConsumedOutboxCacheObserver cacheObserver;

    public ConsumedOutboxManagerCacheDecorator(CacheManager cacheManager, String cacheName, ConsumedOutboxManager delegate,
                                               ConsumedOutboxCacheObserver cacheObserver) {
        Objects.requireNonNull(cacheName, "cacheName cannot be null");
        if (cacheName.isBlank()) {
            throw new IllegalArgumentException("cacheName cannot be empty or blank");
        }
        this.cacheName = cacheName;
        this.cacheManager = cacheManager;
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new IllegalStateException("Cache for outbox with name %s not found".formatted(cacheName));
        }
        this.delegate = delegate;
        this.cacheObserver = cacheObserver;
    }

    @Override
    public boolean isConsumed(UUID id) {
        Cache cache = cacheManager.getCache(cacheName);
        boolean isConsumed;
        if (cache != null) {
            isConsumed = cache.get(id.toString(), String.class) != null;
            if (isConsumed) {
                cacheObserver.onHit();
                return isConsumed;
            }
        } else {
            log.warn("Cache for outbox with name %s not found".formatted(cacheName));
        }
        isConsumed = delegate.isConsumed(id);
        cacheObserver.onMiss();
        if (cache != null) {
            cache.put(id.toString(), "");
        }
        return isConsumed;
    }

    @Override
    public Set<UUID> filterConsumed(Set<UUID> ids) {
        return delegate.filterConsumed(ids);
    }

    @Override
    public int cleanBatchByTtl(Duration ttl, int batchSize) {
        return delegate.cleanBatchByTtl(ttl, batchSize);
    }
}
