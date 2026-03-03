package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

/**
 * Manages the transfer of events between the main outbox table and the DLQ table.
 * <p>
 * These operations are typically executed by background schedulers.
 */
public interface OutboxDlqTransfer {

    /**
     * Transfers a batch of failed outbox events from the main outbox to the DLQ.
     * <p>
     * Events with status {@code FAILED} are moved to the DLQ table with status {@code MOVED}.
     * This operation should be atomic.
     *
     * @param batchSize The maximum number of events to transfer in one operation.
     */
    void transferToDlq(int batchSize);

    /**
     * Transfers a batch of events from the DLQ back to the main outbox for re-processing.
     * <p>
     * Events with status {@code TO_RETRY} in the DLQ are moved back to the outbox table with status {@code PENDING}.
     * Their retry count is reset or incremented depending on the strategy.
     *
     * @param batchSize The maximum number of events to transfer in one operation.
     */
    void transferFromDlq(int batchSize);
}
