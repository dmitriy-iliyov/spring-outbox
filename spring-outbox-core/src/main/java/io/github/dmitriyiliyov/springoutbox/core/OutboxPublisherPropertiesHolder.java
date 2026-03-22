package io.github.dmitriyiliyov.springoutbox.core;

import java.time.Duration;
import java.util.Map;

/**
 * Holds outbox publisher configuration properties.
 * <p>
 * This interface provides access to various settings for event processing, recovery, and DLQ management.
 */
public interface OutboxPublisherPropertiesHolder {

    /**
     * Checks if a specific event type exists in the configuration.
     *
     * @param eventType the event type to check.
     * @return          {@code true} if the event type exists, {@code false} otherwise.
     */
    boolean existEventType(String eventType);

    /**
     * Returns a map of event type to their respective properties holders.
     *
     * @return a map of {@link EventPropertiesHolder}.
     */
    Map<String, EventPropertiesHolder> getEventHolders();

    /**
     * Holds properties for recovering stuck outbox events.
     * <p>
     * Stuck events are those that remain in the {@code IN_PROCESS} state for too long, usually due to a crash.
     */
    interface StuckRecoveryPropertiesHolder {

        /**
         * The maximum time a batch can be in processing before being considered stuck.
         * <p>
         * Events in {@code IN_PROCESS} state longer than this duration will be recovered (moved back to {@code PENDING}).
         */
        Duration getMaxBatchProcessingTime();

        /**
         * The number of stuck events to recover in one batch.
         */
        Integer getBatchSize();

        /**
         * The initial delay before the first stuck recovery operation.
         */
        Duration getInitialDelay();

        /**
         * The fixed delay between subsequent stuck recovery operations.
         */
        Duration getFixedDelay();
    }

    /**
     * Holds properties specific to a particular outbox event type.
     */
    interface EventPropertiesHolder {

        /**
         * The type of the event.
         */
        String getEventType();

        /**
         * The initial delay before the first processing of this event type.
         */
        Duration getInitialDelay();

        /**
         * The fixed delay between subsequent processing operations for this event type.
         */
        Duration getFixedDelay();

        /**
         * The number of events to process in one batch for this event type.
         */
        Integer getBatchSize();

        /**
         * The target topic or exchange name for this event type.
         *
         * @return the topic/exchange name.
         */
        String getTopic();

        /**
         * The maximum number of retries for a failed event of this type.
         */
        Integer getMaxRetries();

        /**
         * The multiplier for exponential backoff retry strategy.
         */
        long backoffMultiplier();

        /**
         * The base delay for exponential backoff retry strategy.
         */
        long backoffDelay();
    }

    /**
     * Holds properties for Dead Letter Queue management.
     */
    interface DlqPropertiesHolder {

        /**
         * The number of DLQ events to process in one batch.
         */
        Integer getBatchSize();

        /**
         * The initial delay before the first transfer of events to the DLQ.
         *
         * @return the initial delay for transfer to DLQ.
         */
        Duration getTransferToInitialDelay();

        /**
         * The fixed delay between subsequent transfers of events to the DLQ.
         *
         * @return the fixed delay for transfer to DLQ.
         */
        Duration getTransferToFixedDelay();

        /**
         * The initial delay before the first transfer of events from the DLQ (for retry).
         *
         * @return the initial delay for transfer from DLQ.
         */
        Duration getTransferFromInitialDelay();

        /**
         * The fixed delay between subsequent transfers of events from the DLQ (for retry).
         *
         * @return the fixed delay for transfer from DLQ.
         */
        Duration getTransferFromFixedDelay();
    }
}
