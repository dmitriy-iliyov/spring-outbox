package io.github.dmitriyiliyov.springoutbox.core;

import java.time.Duration;

/**
 * Holds common outbox system configuration properties.
 * <p>
 * This interface provides access to general settings, such as cleanup properties.
 */
public interface OutboxPropertiesHolder {

    /**
     * Holds properties related to the cleanup of outbox-related data.
     */
    interface CleanUpPropertiesHolder {

        /**
         * The TTL for data before it is eligible for cleanup.
         *
         * @return The duration representing the TTL.
         */
        Duration getTtl();

        /**
         * The number of records to clean up in a single batch operation.
         *
         * @return The batch size for cleanup.
         */
        Integer getBatchSize();

        /**
         * The initial delay before the first cleanup operation.
         *
         * @return The initial delay.
         */
        Duration getInitialDelay();

        /**
         * The fixed delay between subsequent cleanup operations.
         *
         * @return The fixed delay.
         */
        Duration getFixedDelay();
    }
}
