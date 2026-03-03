package io.github.dmitriyiliyov.springoutbox.core.publisher;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;

/**
 * Processes outbox events for a specific event type.
 * <p>
 * This interface defines the contract for the core processing loop:
 * <ol>
 *     <li>Load a batch of events from the database.</li>
 *     <li>Send them to the message broker via {@link OutboxSender}.</li>
 *     <li>Update their status in the database based on the result.</li>
 * </ol>
 */
public interface OutboxProcessor {

    /**
     * Processes a batch of outbox events based on the provided properties.
     * <p>
     * This method is typically called by a scheduler or a background worker.
     * It encapsulates the entire lifecycle of a batch of events from loading to finalization.
     *
     * @param properties Configuration properties for the event type being processed.
     */
    void process(OutboxPublisherPropertiesHolder.EventPropertiesHolder properties);
}
