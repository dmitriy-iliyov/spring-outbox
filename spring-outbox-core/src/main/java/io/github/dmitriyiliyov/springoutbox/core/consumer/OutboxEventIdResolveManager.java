package io.github.dmitriyiliyov.springoutbox.core.consumer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the resolution of outbox event IDs from various raw message types.
 * <p>
 * This component acts as a registry and dispatcher for {@link OutboxEventIdResolver} implementations.
 * It automatically selects the appropriate resolver based on the runtime type of the message.
 */
public interface OutboxEventIdResolveManager {

    /**
     * Resolves the UUID of an outbox event from a single raw message.
     *
     * @param rowMessage The raw message from which to resolve the ID.
     * @param <T>        The type of the raw message.
     * @return           The UUID of the outbox event.
     * @throws           IllegalArgumentException if no suitable resolver is found for the message type.
     */
    <T> UUID resolve(T rowMessage);

    /**
     * Resolves the UUIDs for a list of raw messages.
     * <p>
     * This is useful for batch processing where we need to correlate IDs with messages.
     *
     * @param rowMessages The list of raw messages.
     * @param <T>         The type of the raw messages.
     * @return            A map where keys are the resolved UUIDs and values are the original raw messages.
     * @throws            IllegalArgumentException if no suitable resolver is found.
     */
    <T> Map<UUID, T> resolve(List<T> rowMessages);
}
