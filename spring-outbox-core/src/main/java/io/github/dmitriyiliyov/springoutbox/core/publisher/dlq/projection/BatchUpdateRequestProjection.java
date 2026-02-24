package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.projection;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;

import java.util.Set;
import java.util.UUID;

/**
 * A projection interface for updating the status of a batch of DLQ events.
 */
public interface BatchUpdateRequestProjection {

    /**
     * The set of IDs of the DLQ events to update.
     *
     * @return A set of event UUIDs.
     */
    Set<UUID> ids();

    /**
     * The new status to set for the specified DLQ events.
     *
     * @return The new {@link DlqStatus}.
     */
    DlqStatus status();
}
