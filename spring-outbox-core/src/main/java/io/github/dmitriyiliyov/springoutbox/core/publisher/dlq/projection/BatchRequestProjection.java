package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.projection;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;

/**
 * A projection interface for requesting a batch of DLQ events with pagination.
 */
public interface BatchRequestProjection {

    /**
     * The status of the DLQ events to request.
     *
     * @return the {@link DlqStatus}.
     */
    DlqStatus status();

    /**
     * The page number of the batch to request (0-based).
     *
     * @return the batch number.
     */
    int batchNumber();

    /**
     * The size of the batch to request.
     *
     * @return the batch size.
     */
    int batchSize();
}
