package io.github.dmitriyiliyov.springoutbox.core;

import java.time.Duration;
import java.util.Map;

/**
 * Holds outbox publisher configuration properties.
 * This interface provides access to various settings for event processing, recovery, and DLQ management.
 */
public interface OutboxPublisherPropertiesHolder {

    /**
     * Checks if a specific event type exists in the configuration.
     *
     * @param eventType The event type to check.
     * @return          {@code true} if the event type exists, {@code false} otherwise.
     */
    boolean existEventType(String eventType);

    /**
     * Returns a map of event type to their respective properties holders.
     *
     * @return A map of {@link EventPropertiesHolder}.
     */
    Map<String, EventPropertiesHolder> getEventHolders();

    /**
     * Holds properties for recovering stuck outbox events.
     */
    interface StuckRecoveryPropertiesHolder {

        /**
         * The maximum time a batch can be in processing before being considered stuck.
         *
         * @return The maximum batch processing time.
         */
        Duration getMaxBatchProcessingTime();

        /**
         * The number of stuck events to recover in one batch.
         *
         * @return The batch size for stuck recovery.
         */
        Integer getBatchSize();

        /**
         * The initial delay before the first stuck recovery operation.
         *
         * @return The initial delay.
         */
        Duration getInitialDelay();

        /**
         * The fixed delay between subsequent stuck recovery operations.
         *
         * @return The fixed delay.
         */
        Duration getFixedDelay();
    }

    /**
     * Holds properties specific to a particular outbox event type.
     */
    interface EventPropertiesHolder {

        /**
         * The type of the event.
         *
         * @return The event type string.
         */
        String getEventType();

        /**
         * The initial delay before the first processing of this event type.
         *
         * @return The initial delay.
         */
        Duration getInitialDelay();

        /**
         * The fixed delay between subsequent processing operations for this event type.
         *
         * @return The fixed delay.
         */
        Duration getFixedDelay();

        /**
         * The number of events to process in one batch for this event type.
         *
         * @return The batch size.
         */
        Integer getBatchSize();

        /**
         * The target topic or exchange name for this event type.
         *
         * @return The topic/exchange name.
         */
        String getTopic();

        /**
         * The maximum number of retries for a failed event of this type.
         *
         * @return The maximum retry count.
         */
        Integer getMaxRetries();

        /**
         * The multiplier for exponential backoff retry strategy.
         *
         * @return The backoff multiplier.
         */
        long backoffMultiplier();

        /**
         * The base delay for exponential backoff retry strategy.
         *
         * @return The backoff delay.
         */
        long backoffDelay();
    }

    /**
     * Holds properties for Dead Letter Queue management.
     */
    interface DlqPropertiesHolder {

        /**
         * The number of DLQ events to process in one batch.
         *
         * @return The batch size for DLQ operations.
         */
        Integer getBatchSize();

        /**
         * The initial delay before the first transfer of events to the DLQ.
         *
         * @return The initial delay for transfer to DLQ.
         */
        Duration getTransferToInitialDelay();

        /**
         * The fixed delay between subsequent transfers of events to the DLQ.
         *
         * @return The fixed delay for transfer to DLQ.
         */
        Duration getTransferToFixedDelay();

        /**
         * The initial delay before the first transfer of events from the DLQ (for retry).
         *
         * @return The initial delay for transfer from DLQ.
         */
        Duration getTransferFromInitialDelay();

        /**
         * The fixed delay between subsequent transfers of events from the DLQ (for retry).
         *
         * @return The fixed delay for transfer from DLQ.
         */
        Duration getTransferFromFixedDelay();
    }
}
