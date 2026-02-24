package io.github.dmitriyiliyov.springoutbox.core.consumer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages and delegates the resolution of outbox event IDs from various raw message types.
 * It uses registered {@link OutboxEventIdResolver} implementations to find the correct ID.
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
     * Resolves the UUIDs for a list of raw messages, returning a map of UUIDs to their corresponding messages.
     *
     * @param rowMessages The list of raw messages.
     * @param <T>         The type of the raw messages.
     * @return            A map where keys are the resolved UUIDs and values are the original raw messages.
     * @throws            IllegalArgumentException if no suitable resolver is found for any message type.
     */
    <T> Map<UUID, T> resolve(List<T> rowMessages);
}
