package io.github.dmitriyiliyov.springoutbox.core.consumer;

import java.util.UUID;

/**
 * Strategy interface for resolving the unique ID of an outbox event from a raw message.
 * <p>
 * This is used by the idempotent consumer to extract the event ID from headers
 * without knowing the specific message broker implementation.
 *
 * @param <T> The type of the raw message from which to resolve the event ID.
 */
public interface OutboxEventIdResolver<T> {

    /**
     * Resolves the UUID of an outbox event from the given raw message.
     * <p>
     * Implementations should look for the ID in message headers or payload.
     *
     * @param rowMessage The raw message containing the event.
     * @return           The UUID of the outbox event.
     * @throws           IllegalArgumentException if the ID cannot be resolved.
     */
    UUID resolve(T rowMessage);

    /**
     * Returns the class type of the message that this resolver supports.
     * <p>
     * Used by the {@link OutboxEventIdResolveManager} to select the appropriate resolver at runtime.
     *
     * @return The supported message class type.
     */
    Class<?> getSupports();
}
