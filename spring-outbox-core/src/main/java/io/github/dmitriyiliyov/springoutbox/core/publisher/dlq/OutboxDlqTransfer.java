package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

/**
 * Manages the transfer of events between the main outbox and the DLQ.
 * This interface defines operations for moving failed events to the DLQ and potentially
 * retrying them from the DLQ.
 */
public interface OutboxDlqTransfer {

    /**
     * Transfers a batch of failed outbox events from the main outbox to the DLQ.
     *
     * @param batchSize The maximum number of events to transfer in one operation.
     */
    void transferToDlq(int batchSize);

    /**
     * Transfers a batch of events from the DLQ back to the main outbox for re-processing.
     *
     * @param batchSize The maximum number of events to transfer in one operation.
     */
    void transferFromDlq(int batchSize);
}
