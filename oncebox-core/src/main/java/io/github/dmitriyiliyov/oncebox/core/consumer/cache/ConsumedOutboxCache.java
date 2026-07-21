package io.github.dmitriyiliyov.oncebox.core.consumer.cache;

import java.util.UUID;

/**
 * A cache for checking if an outbox event has already been consumed.
 * <p>
 * This interface provides methods to verify the consumption status of an event
 * and to mark an event as consumed, helping to ensure idempotent processing.
 */
public interface ConsumedOutboxCache {

    /**
     * Checks whether the event with the given ID has already been consumed.
     *
     * @param id the unique identifier of the event.
     * @return   {@code true} if the event is present in the cache (consumed), {@code false} otherwise.
     */
    boolean isConsumed(UUID id);

    /**
     * Marks the event with the given ID as consumed by adding it to the cache.
     *
     * @param id the unique identifier of the event to cache.
     */
    void consume(UUID id);

    /**
     * A no-operation implementation that does nothing.
     */
    ConsumedOutboxCache NOOP_CACHE = new ConsumedOutboxCache() {
        @Override
        public boolean isConsumed(UUID id) {
            return false;
        }

        @Override
        public void consume(UUID id) { }
    };
}
