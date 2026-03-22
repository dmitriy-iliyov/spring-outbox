package io.github.dmitriyiliyov.springoutbox.core.consumer;

import java.util.List;
import java.util.function.Consumer;

/**
 * Ensures idempotent message consumption by tracking processed event IDs.
 * <p>
 * The event ID is extracted from each message using a registered {@link OutboxEventIdResolver}.
 */
public interface OutboxIdempotentConsumer {

    /**
     * Executes the operation only if the message has not been processed before.
     * <p>
     * If the message has already been consumed, the operation is skipped.
     *
     * @param message   the message to consume (e.g., a Kafka or AMQP message).
     * @param operation the business logic to execute if the message is new.
     * @param <T>       the type of the message.
     * @throws IllegalArgumentException if no resolver is registered for the message type.
     */
    <T> void consume(T message, Runnable operation);

    /**
     * Executes the operation for the subset of messages that have not been processed before.
     * <p>
     * Already consumed messages are filtered out before the operation is called.
     * If all messages have already been consumed, the operation is not called at all.
     * Does nothing if the list is null or empty.
     *
     * @param messages  the list of messages to consume.
     * @param operation the business logic to execute with the list of new messages.
     * @param <T>       the type of the messages.
     * @throws IllegalArgumentException if no resolver is registered for the message type.
     */
    <T> void consume(List<T> messages, Consumer<List<T>> operation);
}
