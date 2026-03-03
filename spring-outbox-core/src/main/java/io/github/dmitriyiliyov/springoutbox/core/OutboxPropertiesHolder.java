package io.github.dmitriyiliyov.springoutbox.core;

import java.time.Duration;

/**
 * Holds common outbox system configuration properties.
 * <p>
 * This interface provides access to general settings that apply across the entire outbox system.
 */
public interface OutboxPropertiesHolder {

    /**
     * Holds properties related to the cleanup of outbox-related data.
     */
    interface CleanUpPropertiesHolder {

        /**
         * The TTL for data before it is eligible for cleanup.
         * <p>
         * Events older than this duration will be deleted by the cleanup job.
         *
         * @return The duration representing the TTL.
         */
        Duration getTtl();

        /**
         * The number of records to clean up in a single batch operation.
         * <p>
         * This controls the transaction size during cleanup to avoid locking the database for too long.
         *
         * @return The batch size for cleanup.
         */
        Integer getBatchSize();

        /**
         * The initial delay before the first cleanup operation starts after application startup.
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
