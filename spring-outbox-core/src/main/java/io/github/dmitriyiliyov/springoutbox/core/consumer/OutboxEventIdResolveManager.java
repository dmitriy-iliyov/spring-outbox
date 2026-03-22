package io.github.dmitriyiliyov.springoutbox.core.consumer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the resolution of outbox event IDs from various raw message types.
 * <p>
 * Acts as a registry and dispatcher for {@link OutboxEventIdResolver} implementations,
 * automatically selecting the appropriate resolver based on the runtime type of the message.
 */
public interface OutboxEventIdResolveManager {

    /**
     * Resolves the UUID of an outbox event from a single raw message.
     *
     * @param rawMessage the raw message from which to resolve the event ID.
     * @param <T>        the type of the raw message.
     * @return           the UUID of the outbox event.
     * @throws IllegalArgumentException if no resolver is registered for the message type,
     *                                  or if the required header is missing or malformed.
     */
    <T> UUID resolve(T rawMessage);

    /**
     * Resolves the UUIDs for a list of raw messages of the same type.
     * <p>
     * Returns an empty map if the list is null or empty.
     *
     * @param rawMessages the list of raw messages.
     * @param <T>         the type of the raw messages.
     * @return            a map where keys are resolved UUIDs and values are the corresponding raw messages.
     * @throws IllegalArgumentException if no resolver is registered for the message type,
     *                                  or if any required header is missing or malformed.
     */
    <T> Map<UUID, T> resolve(List<T> rawMessages);
}
