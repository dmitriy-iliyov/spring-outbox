package io.github.dmitriyiliyov.springoutbox.core.consumer;

import java.util.UUID;

/**
 * Resolves the unique ID of an outbox event from a raw message.
 *
 * @param <T> The type of the raw message from which to resolve the event ID.
 */
public interface OutboxEventIdResolver<T> {

    /**
     * Resolves the UUID of an outbox event from the given raw message.
     *
     * @param rowMessage The raw message containing the event.
     * @return           The UUID of the outbox event.
     */
    UUID resolve(T rowMessage);

    /**
     * Returns the class type that this resolver supports.
     *
     * @return The supported class type.
     */
    Class<?> getSupports();
}
