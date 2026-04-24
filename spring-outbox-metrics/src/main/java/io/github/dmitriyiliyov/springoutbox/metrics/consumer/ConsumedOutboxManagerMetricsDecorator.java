package io.github.dmitriyiliyov.springoutbox.metrics.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

public class ConsumedOutboxManagerMetricsDecorator implements ConsumedOutboxManager {

    private final Counter rejectedDuplicates;
    private final Counter consumed;
    private final Counter cleaned;
    private final ConsumedOutboxManager delegate;

    public ConsumedOutboxManagerMetricsDecorator(MeterRegistry registry, ConsumedOutboxManager delegate) {
        this.rejectedDuplicates = registry.counter("consumed_outbox_events_total", "type", "rejected_duplicates");
        this.consumed = registry.counter("consumed_outbox_events_total", "type", "consumed");
        this.cleaned = registry.counter("consumed_outbox_events_total", "type", "cleaned");
        this.delegate = delegate;
    }

    @Override
    public boolean isConsumed(UUID id) {
        boolean isConsumed = delegate.isConsumed(id);
        if (isConsumed) {
            rejectedDuplicates.increment();
        } else {
            consumed.increment();
        }
        return isConsumed;
    }

    @Override
    public Set<UUID> filterConsumed(Set<UUID> ids) {
        Set<UUID> alreadyConsumed = delegate.filterConsumed(ids);
        double currentDuplicates = alreadyConsumed.size();
        rejectedDuplicates.increment(currentDuplicates);
        consumed.increment(ids.size() - currentDuplicates);
        return alreadyConsumed;
    }

    @Override
    public int cleanBatchByTtl(Duration ttl, int batchSize) {
        int cleanedCount = delegate.cleanBatchByTtl(ttl, batchSize);
        cleaned.increment(cleanedCount);
        return cleanedCount;
    }
}
