package io.github.dmitriyiliyov.springoutbox.metrics.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OutboxDlqManagerMetricsDecorator implements OutboxDlqManager {

    private final Map<AdditionalCounterType, Counter> additionalCounters;
    private final OutboxDlqManager delegate;

    public OutboxDlqManagerMetricsDecorator(MeterRegistry registry, OutboxDlqManager delegate) {
        this.additionalCounters = Arrays.stream(AdditionalCounterType.values())
                .collect(Collectors.toMap(
                                Function.identity(),
                                type -> registry.counter(
                                        "outbox_dlq_events_by_type_rate_total",
                                        "type", type.toString().toLowerCase())
                        )
                );
        this.delegate = delegate;
    }

    @Override
    public void saveBatch(List<OutboxDlqEvent> events) {
        delegate.saveBatch(events);
    }

    @Override
    public List<OutboxDlqEvent> loadAndLockBatch(DlqStatus status, int batchSize) {
        List<OutboxDlqEvent> events = delegate.loadAndLockBatch(status, batchSize);
        if (events != null && !events.isEmpty()) {
            additionalCounters.get(AdditionalCounterType.ATTEMPT_MOVE_TO_OUTBOX).increment(events.size());
        }
        return events;
    }

    @Override
    public int deleteBatch(Set<UUID> ids) {
        int deletedCount = delegate.deleteBatch(ids);
        additionalCounters.get(AdditionalCounterType.SUCCESS_MOVED_TO_OUTBOX).increment(deletedCount);
        return deletedCount;
    }
}
