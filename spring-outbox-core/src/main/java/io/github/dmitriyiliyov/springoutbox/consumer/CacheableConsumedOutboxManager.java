package io.github.dmitriyiliyov.springoutbox.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Objects;
import java.util.UUID;

public class CacheableConsumedOutboxManager extends DefaultConsumedOutboxManager {

    private static final Logger log = LoggerFactory.getLogger(CacheableConsumedOutboxManager.class);

    private final String cacheName;
    private final CacheManager cacheManager;

    public CacheableConsumedOutboxManager(CacheManager cacheManager, String cacheName, ConsumedOutboxRepository repository) {
        super(repository);
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
    }

    @Override
    public boolean isConsumed(UUID id) {
        Cache cache = cacheManager.getCache(cacheName);
        boolean isConsumed;
        if (cache != null) {
            isConsumed = cache.get(id.toString(), String.class) != null;
            if (isConsumed) {
                return isConsumed;
            }
        } else {
            log.error("Cache for outbox with name %s not found".formatted(cacheName));
        }
        isConsumed = repository.saveIfAbsent(id) == 0;
        if (cache != null) {
            cache.put(id.toString(), "");
        }
        return isConsumed;
    }
}
