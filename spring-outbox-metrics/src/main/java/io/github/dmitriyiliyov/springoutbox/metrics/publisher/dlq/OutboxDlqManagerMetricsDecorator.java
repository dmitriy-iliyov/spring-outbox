package io.github.dmitriyiliyov.springoutbox.metrics.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OutboxDlqManagerMetricsDecorator implements OutboxDlqManager {

    private final Map<ActionType, Counter> actionCounters;
    private final OutboxDlqManager delegate;

    public OutboxDlqManagerMetricsDecorator(MeterRegistry registry, OutboxDlqManager delegate) {
        this.actionCounters = Arrays.stream(ActionType.values())
                .collect(Collectors.toMap(
                                Function.identity(),
                                type -> registry.counter(
                                        "outbox_dlq_events_by_action_type_rate_total",
                                        "action_type", type.toString().toLowerCase())
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
            actionCounters.get(ActionType.ATTEMPT_MOVE_TO_OUTBOX).increment(events.size());
        }
        return events;
    }

    @Override
    public int deleteBatch(Set<UUID> ids) {
        int deletedCount = delegate.deleteBatch(ids);
        actionCounters.get(ActionType.SUCCESS_MOVED_TO_OUTBOX).increment(deletedCount);
        return deletedCount;
    }

    @Override
    public int deleteResolvedBatch(Instant threshold, int batchSize) {
        int deletedCount = delegate.deleteResolvedBatch(threshold, batchSize);
        actionCounters.get(ActionType.CLEANED).increment(deletedCount);
        return deletedCount;
    }
}
