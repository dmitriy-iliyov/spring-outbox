package io.github.dmitriyiliyov.springoutbox.metrics.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.springoutbox.dlq.api.BatchRequest;
import io.github.dmitriyiliyov.springoutbox.dlq.api.BatchUpdateRequest;
import io.github.dmitriyiliyov.springoutbox.dlq.api.OutboxDlqApiManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OutboxDlqApiManagerMetricsDecorator implements OutboxDlqApiManager {

    private final Map<ActionType, Counter> actionCounters;
    private final OutboxDlqApiManager delegate;

    public OutboxDlqApiManagerMetricsDecorator(MeterRegistry registry, OutboxDlqApiManager delegate) {
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
    public OutboxDlqEvent findById(UUID id) {
        return delegate.findById(id);
    }

    @Override
    public List<OutboxDlqEvent> findBatch(BatchRequest request) {
        return delegate.findBatch(request);
    }

    @Override
    public long count(DlqStatus status) {
        return delegate.count(status);
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
        actionCounters.get(ActionType.MANUAL_DELETED).increment(deletedCount);
        return deletedCount;
    }

    @Override
    public int deleteBatch(Set<UUID> ids) {
        int deletedCount = delegate.deleteBatch(ids);
        actionCounters.get(ActionType.MANUAL_DELETED).increment(deletedCount);
        return deletedCount;
    }
}
