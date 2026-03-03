package io.github.dmitriyiliyov.springoutbox.core.consumer;

import java.util.List;
import java.util.function.Consumer;

/**
 * Provides idempotent consumption of messages by tracking processed event IDs.
 * <p>
 * This interface ensures that a given operation is executed only once for each unique event ID.
 * It relies on an {@link OutboxEventIdResolver} to extract the unique ID from the message (e.g., from headers).
 */
public interface OutboxIdempotentConsumer {

    /**
     * Consumes a single message idempotently.
     * <p>
     * The provided operation will be executed only if the message's ID has not been consumed before.
     * If the message has already been processed, the operation is skipped.
     *
     * @param message   The message to consume. Its ID will be resolved by an {@link OutboxEventIdResolver}.
     * @param operation The operation to execute if the message is new.
     * @param <T>       The type of the message.
     */
    <T> void consume(T message, Runnable operation);

    /**
     * Consumes a list of messages idempotently.
     * <p>
     * The provided operation will be executed only for the subset of messages whose IDs have not been consumed before.
     * Already processed messages are filtered out before the operation is called.
     *
     * @param messages  The list of messages to consume. Their IDs will be resolved.
     * @param operation The operation to execute for the list of new messages.
     * @param <T>       The type of the messages.
     */
    <T> void consume(List<T> messages, Consumer<List<T>> operation);
}
