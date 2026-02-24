package io.github.dmitriyiliyov.springoutbox.core.consumer;

/**
 * Abstraction for monitoring cache performance for consumed outbox events.
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
