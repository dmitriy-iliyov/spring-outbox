package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxCacheObserver;
import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxManagerCacheDecorator;
import org.springframework.cache.CacheManager;

public final class ConsumedOutboxManagerCacheDecoratorSupplier implements ConsumedOutboxManagerDecoratorSupplier {

    private final String cacheName;
    private final CacheManager cacheManager;
    private final ConsumedOutboxCacheObserver cacheObserver;

    public ConsumedOutboxManagerCacheDecoratorSupplier(CacheManager cacheManager,
                                                       String cacheName,
                                                       ConsumedOutboxCacheObserver cacheObserver) {
        this.cacheManager = cacheManager;
        this.cacheName = cacheName;
        this.cacheObserver = cacheObserver;
    }

    @Override
    public ConsumedOutboxManager supply(ConsumedOutboxManager manager) {
        return new ConsumedOutboxManagerCacheDecorator(cacheManager, cacheName, manager, cacheObserver);
    }
}
