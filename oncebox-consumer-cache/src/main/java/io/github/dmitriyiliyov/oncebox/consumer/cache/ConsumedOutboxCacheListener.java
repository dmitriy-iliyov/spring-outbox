package io.github.dmitriyiliyov.oncebox.consumer.cache;

/**
 * Abstraction for monitoring cache hits and misses for consumed outbox events.
 */
public interface ConsumedOutboxCacheListener {

    void onHit();

    void onMiss();
    
    /**
     * A no-operation implementation that does nothing on hit or miss.
     */
    ConsumedOutboxCacheListener NOOP = new ConsumedOutboxCacheListener() {
        @Override
        public void onHit() { }

        @Override
        public void onMiss() { }
    };
}
