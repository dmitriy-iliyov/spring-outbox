package io.github.dmitriyiliyov.springoutbox.publisher.metrics;

import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherProperties;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.OutboxDlqManager;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.dto.BatchRequest;
import io.github.dmitriyiliyov.springoutbox.publisher.dlq.dto.BatchUpdateRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OutboxDlqManagerMetricsDecorator implements OutboxDlqManager {

    private final OutboxDlqManager delegate;
    private final Map<String, Map<DlqStatus, Counter>> counters;
    private final Map<AdditionalCounterType, Counter> additionalCounters;

    public OutboxDlqManagerMetricsDecorator(OutboxDlqManager delegate,
                                            OutboxPublisherProperties properties,
                                            MeterRegistry registry) {
        this.delegate = delegate;
        this.counters = properties.getEvents().keySet().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        eventType -> Arrays.stream(DlqStatus.values())
                                .collect(Collectors.toMap(
                                        Function.identity(),
                                        status -> registry.counter(
                                                "outbox_dlq_events_rate_total",
                                                "event_type", eventType,
                                                "status", status.toString().toLowerCase())
                                        )
                                ))
                );
        this.additionalCounters = Arrays.stream(AdditionalCounterType.values())
                .collect(Collectors.toMap(
                        Function.identity(),
                        type -> registry.counter(
                                "outbox_dlq_events_by_type_rate_total",
                                "type", type.toString().toLowerCase())
                        )
                );
    }

    @Override
    public void saveBatch(List<OutboxDlqEvent> events) {
        delegate.saveBatch(events);
        CompletableFuture.runAsync(
                () -> {
                    if (!events.isEmpty()) {
                        for(OutboxDlqEvent event : events) {
                            Map<DlqStatus, Counter> countersByDlqStatus = counters.get(event.getEventType());
                            if (countersByDlqStatus != null) {
                                Counter c = countersByDlqStatus.get(event.getDlqStatus());
                                if (c != null) {
                                    c.increment();
                                }
                            }
                        }
                    }
                }
        );
    }

    @Override
    public long count() {
        return delegate.count();
    }

    @Override
    public long countByStatus(DlqStatus status) {
        return delegate.countByStatus(status);
    }

    @Override
    public long countByEventTypeAndStatus(String eventType, DlqStatus status) {
        return delegate.countByEventTypeAndStatus(eventType, status);
    }

    @Override
    public OutboxDlqEvent loadById(UUID id) {
        return delegate.loadById(id);
    }

    @Override
    public List<OutboxDlqEvent> loadAndLockBatch(DlqStatus status, int batchSize) {
        List<OutboxDlqEvent> events = delegate.loadAndLockBatch(status, batchSize);
        additionalCounters.get(AdditionalCounterType.ATTEMPT_MOVE_TO_OUTBOX).increment(events.size());
        return events;
    }

    @Override
    public List<OutboxDlqEvent> loadBatch(BatchRequest request) {
        return delegate.loadBatch(request);
    }

    @Override
    public void updateStatus(UUID id, DlqStatus status) {
        delegate.updateStatus(id, status);
    }

    @Override
    public void updateBatchStatus(BatchUpdateRequest request) {
        delegate.updateBatchStatus(request);
    }

    @Override
    public int deleteById(UUID id) {
        int deletedCount = delegate.deleteById(id);
        additionalCounters.get(AdditionalCounterType.MANUAL_DELETED).increment(deletedCount);
        return deletedCount;
    }

    @Override
    public int deleteBatch(Set<UUID> ids) {
        int deletedCount = delegate.deleteBatch(ids);
        additionalCounters.get(AdditionalCounterType.SUCCESS_MOVED_TO_OUTBOX).increment(deletedCount);
        return deletedCount;
    }

    @Override
    public int deleteBatchWithCheck(Set<UUID> ids) {
        int deletedCount = delegate.deleteBatchWithCheck(ids);
        additionalCounters.get(AdditionalCounterType.MANUAL_DELETED).increment(deletedCount);
        return deletedCount;
    }

    private enum AdditionalCounterType {
        ATTEMPT_MOVE_TO_OUTBOX, SUCCESS_MOVED_TO_OUTBOX, MANUAL_DELETED;
    }
}
