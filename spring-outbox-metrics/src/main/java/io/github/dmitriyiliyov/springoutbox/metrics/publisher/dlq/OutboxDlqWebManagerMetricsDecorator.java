package io.github.dmitriyiliyov.springoutbox.metrics.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.springoutbox.web.BatchRequest;
import io.github.dmitriyiliyov.springoutbox.web.BatchUpdateRequest;
import io.github.dmitriyiliyov.springoutbox.web.OutboxDlqWebManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OutboxDlqWebManagerMetricsDecorator implements OutboxDlqWebManager {

    private final Map<AdditionalCounterType, Counter> additionalCounters;
    private final OutboxDlqWebManager delegate;

    public OutboxDlqWebManagerMetricsDecorator(MeterRegistry registry, OutboxDlqWebManager delegate) {
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
        additionalCounters.get(AdditionalCounterType.MANUAL_DELETED).increment(deletedCount);
        return deletedCount;
    }

    @Override
    public int deleteBatch(Set<UUID> ids) {
        int deletedCount = delegate.deleteBatch(ids);
        additionalCounters.get(AdditionalCounterType.MANUAL_DELETED).increment(deletedCount);
        return deletedCount;
    }
}
