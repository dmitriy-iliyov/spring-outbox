package io.github.dmitriyiliyov.springoutbox.core.publisher.metrics;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OutboxManagerMetricsDecorator implements OutboxManager {

    private static final EventStatus [] STATUSES = new EventStatus[] {EventStatus.PROCESSED, EventStatus.FAILED};
    private final OutboxManager delegate;
    private final Map<String, Map<EventStatus, Counter>> counters;
    private final Map<AdditionalCounterType, Counter> additionalCounters;

    public OutboxManagerMetricsDecorator(OutboxManager delegate, OutboxPublisherPropertiesHolder properties,
                                         MeterRegistry registry) {
        this.delegate = delegate;
        this.counters = properties.getEventHolders().keySet()
                .stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        eventType -> Arrays.stream(STATUSES)
                                .collect(Collectors.toMap(
                                        Function.identity(),
                                        eventStatus -> registry.counter(
                                                "outbox_events_rate_total",
                                                "event_type", eventType,
                                                "status", eventStatus.toString().toLowerCase()
                                        )
                                ))
                ));
        this.additionalCounters = Arrays.stream(AdditionalCounterType.values())
                .collect(Collectors.toMap(
                        Function.identity(),
                        type -> registry.counter(
                                "outbox_events_by_type_rate_total",
                                "type", type.toString().toLowerCase())
                        )
                );
    }

    @Override
    public void save(OutboxEvent event) {
        delegate.save(event);
    }

    @Override
    public void saveBatch(List<OutboxEvent> eventBatch) {
        delegate.saveBatch(eventBatch);
    }

    @Override
    public long count() {
        return delegate.count();
    }

    @Override
    public long countByStatus(EventStatus status) {
        return delegate.countByStatus(status);
    }

    @Override
    public long countByEventTypeAndStatus(String eventType, EventStatus status) {
        return delegate.countByEventTypeAndStatus(eventType, status);
    }

    @Override
    public List<OutboxEvent> loadBatch(String eventType, int batchSize) {
        return delegate.loadBatch(eventType, batchSize);
    }

    @Override
    public List<OutboxEvent> loadBatch(EventStatus status, int batchSize) {
        List<OutboxEvent> events = delegate.loadBatch(status, batchSize);
        additionalCounters.get(AdditionalCounterType.ATTEMPT_MOVE_TO_DLQ).increment(events.size());
        return events;
    }

    @Override
    public void finalizeBatch(List<OutboxEvent> events, Set<UUID> processedIds, Set<UUID> failedIds,
                              int maxRetryCount, Function<Integer, Instant> nextRetryAtSupplier) {
        delegate.finalizeBatch(events, processedIds, failedIds, maxRetryCount, nextRetryAtSupplier);
        if (!events.isEmpty()) {
            String eventType = events.getFirst().getEventType();
            if (eventType != null) {
                Map<EventStatus, Counter> eventTypeCounter = counters.get(eventType);
                if (processedIds != null) {
                    eventTypeCounter.get(EventStatus.PROCESSED).increment(processedIds.size());
                }
                if (failedIds != null) {
                    eventTypeCounter.get(EventStatus.FAILED).increment(failedIds.size());
                }
            }
        }
    }

    @Override
    public int recoverStuckBatch(Duration maxBatchProcessingTime, int batchSize) {
        int recoveredCount = delegate.recoverStuckBatch(maxBatchProcessingTime, batchSize);
        additionalCounters.get(AdditionalCounterType.RECOVERED).increment(recoveredCount);
        return recoveredCount;
    }

    @Override
    public int deleteProcessedBatch(Instant threshold, int batchSize) {
        int deletedCount = delegate.deleteProcessedBatch(threshold, batchSize);
        additionalCounters.get(AdditionalCounterType.CLEANED).increment(deletedCount);
        return deletedCount;
    }

    @Override
    public int deleteBatch(Set<UUID> ids) {
        int deletedCount = delegate.deleteBatch(ids);
        additionalCounters.get(AdditionalCounterType.SUCCESS_MOVED_TO_DLQ).increment(deletedCount);
        return deletedCount;
    }

    private enum AdditionalCounterType {
        ATTEMPT_MOVE_TO_DLQ, RECOVERED, CLEANED, SUCCESS_MOVED_TO_DLQ;
    }
}
