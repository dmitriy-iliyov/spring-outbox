package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxCacheListener;
import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxManagerCacheDecorator;
import org.springframework.cache.CacheManager;

public final class ConsumedOutboxManagerCacheDecoratorSupplier implements ConsumedOutboxManagerDecoratorSupplier {

    private final String cacheName;
    private final CacheManager cacheManager;
    private final ConsumedOutboxCacheListener cacheListener;

    public ConsumedOutboxManagerCacheDecoratorSupplier(CacheManager cacheManager,
                                                       String cacheName,
                                                       ConsumedOutboxCacheListener cacheListener) {
        this.cacheManager = cacheManager;
        this.cacheName = cacheName;
        this.cacheListener = cacheListener;
    }

    @Override
    public ConsumedOutboxManager supply(ConsumedOutboxManager manager) {
        return new ConsumedOutboxManagerCacheDecorator(cacheManager, cacheName, manager, cacheListener);
    }
}
