package io.github.dmitriyiliyov.springoutbox.core.consumer;

import java.util.UUID;

/**
 * Strategy interface for extracting the unique ID of an outbox event from a raw message.
 * <p>
 * This is used by {@link DefaultOutboxIdempotentConsumer} to extract the event ID from headers
 * without knowing the specific message broker implementation.
 *
 * @param <T> The type of the raw message from which to resolve the event ID.
 */
@FunctionalInterface
public interface OutboxEventIdExtractor<T> {

    /**
     * Extracts the UUID of an outbox event from the given raw message.
     *
     * @param message the raw message containing the event.
     * @return        the UUID of the outbox event.
     */
    UUID extract(T message);
}
