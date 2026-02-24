package io.github.dmitriyiliyov.springoutbox.core.publisher;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;

/**
 * Processes outbox events for a specific event type.
 * <p>
 * This interface defines the contract for how events are loaded, sent, and their status updated.
 */
public interface OutboxProcessor {

    /**
     * Processes a batch of outbox events based on the provided properties.
     *
     * @param properties Configuration properties for the event type being processed.
     */
    void process(OutboxPublisherPropertiesHolder.EventPropertiesHolder properties);
}
