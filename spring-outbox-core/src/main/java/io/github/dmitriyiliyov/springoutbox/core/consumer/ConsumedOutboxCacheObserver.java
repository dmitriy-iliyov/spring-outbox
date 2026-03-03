package io.github.dmitriyiliyov.springoutbox.core.consumer;

/**
 * Observer interface for monitoring cache hits and misses for consumed outbox events.
 * <p>
 * This is typically used for collecting metrics on cache efficiency.
 */
public interface ConsumedOutboxCacheObserver {

    /**
     * Called when a cache hit occurs.
     */
    void onHit();

    /**
     * Called when a cache miss occurs.
     */
    void onMiss();
}
