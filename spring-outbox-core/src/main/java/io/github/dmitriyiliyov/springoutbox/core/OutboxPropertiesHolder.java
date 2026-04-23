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
    interface CleanUpPropertiesHolder extends PollingPropertiesHolder {

        /**
         * The TTL for data before it is eligible for cleanup.
         * <p>
         * Events older than this duration will be deleted by the cleanup job.
         */
        Duration getTtl();

        /**
         * The number of records to clean up in a single batch operation.
         * <p>
         * This controls the transaction size during cleanup to avoid locking the database for too long.
         */
        Integer getBatchSize();
    }
}
