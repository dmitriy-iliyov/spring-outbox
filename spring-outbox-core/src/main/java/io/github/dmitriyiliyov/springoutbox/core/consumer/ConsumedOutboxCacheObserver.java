package io.github.dmitriyiliyov.springoutbox.core.consumer;

/**
 * Abstraction for monitoring cache hits and misses for consumed outbox events.
 */
public interface ConsumedOutboxCacheObserver {

    void onHit();

    void onMiss();
}
