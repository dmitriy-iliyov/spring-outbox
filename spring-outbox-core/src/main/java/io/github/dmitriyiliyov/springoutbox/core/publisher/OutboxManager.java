package io.github.dmitriyiliyov.springoutbox.core.publisher;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Manages the lifecycle of outbox events, including saving, loading, and updating their state.
 * <p>
 * This interface acts as a service layer between the application/publisher and the persistence layer.
 * It handles business logic related to event state transitions (e.g., retry calculation, stuck event recovery).
 */
public interface OutboxManager {

    /**
     * Saves a single outbox event.
     * <p>
     * This method should be called within an existing transaction (MANDATORY propagation)
     * to ensure atomicity with the business operation.
     *
     * @param event The event to save.
     */
    void save(OutboxEvent event);

    /**
     * Saves a batch of outbox events.
     * <p>
     * This method should be called within an existing transaction (MANDATORY propagation).
     *
     * @param eventBatch The list of events to save.
     */
    void saveBatch(List<OutboxEvent> eventBatch);

    /**
     * Loads and locks a batch of events of a specific type for processing.
     * <p>
     * Events are locked (status changed to IN_PROCESS) to prevent concurrent processing by other instances.
     *
     * @param eventType The type of events to load.
     * @param batchSize The maximum number of events to load.
     * @return          A list of locked outbox events ready for processing.
     */
    List<OutboxEvent> loadBatch(String eventType, int batchSize);

    /**
     * Loads and locks a batch of events with a specific status.
     * <p>
     * Typically used for maintenance tasks like DLQ processing or cleanup.
     *
     * @param status    The status of events to load.
     * @param batchSize The maximum number of events to load.
     * @return          A list of locked outbox events.
     */
    List<OutboxEvent> loadBatch(EventStatus status, int batchSize);

    /**
     * Finalizes a batch of events after processing, marking them as processed or failed.
     * <p>
     * This method updates the status of events based on the processing result.
     * Failed events will have their retry count incremented and next retry time calculated.
     * If max retries are exhausted, the event is marked as FAILED.
     *
     * @param events              The original list of events in the batch.
     * @param processedIds        The IDs of successfully processed events.
     * @param failedIds           The IDs of failed events.
     * @param maxRetryCount       The maximum number of retries for failed events.
     * @param nextRetryAtSupplier A function to calculate the next retry time (e.g., exponential backoff).
     */
    void finalizeBatch(List<OutboxEvent> events, Set<UUID> processedIds, Set<UUID> failedIds,
                       int maxRetryCount, Function<Integer, Instant> nextRetryAtSupplier);

    /**
     * Recovers events that are stuck in the {@code IN_PROCESS} state for too long.
     * <p>
     * These events are moved back to {@code PENDING} state to be picked up again.
     * This handles cases where a processing node crashes.
     *
     * @param maxBatchProcessingTime The maximum time a batch can be in processing before being considered stuck.
     * @param batchSize              The number of events to recover in one go.
     * @return                       The number of recovered events.
     */
    int recoverStuckBatch(Duration maxBatchProcessingTime, int batchSize);

    /**
     * Deletes processed events that are older than a given threshold.
     *
     * @param threshold The time threshold for deletion (events older than this will be deleted).
     * @param batchSize The number of events to delete in one go.
     * @return          The number of deleted events.
     */
    int deleteProcessedBatch(Instant threshold, int batchSize);

    /**
     * Deletes a batch of events by their IDs.
     *
     * @param ids The set of event IDs to delete.
     * @return    The number of deleted events.
     */
    int deleteBatch(Set<UUID> ids);
}
