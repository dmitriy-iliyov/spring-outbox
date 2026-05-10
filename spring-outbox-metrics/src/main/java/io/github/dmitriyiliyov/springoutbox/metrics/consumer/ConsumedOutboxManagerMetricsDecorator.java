package io.github.dmitriyiliyov.springoutbox.metrics.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class ConsumedOutboxManagerMetricsDecorator implements ConsumedOutboxManager {

    private final Counter rejectedDuplicates;
    private final Counter consumed;
    private final Counter cleaned;
    private final ConsumedOutboxManager delegate;

    public ConsumedOutboxManagerMetricsDecorator(MeterRegistry registry, ConsumedOutboxManager delegate) {
        Objects.requireNonNull(registry, "registry cannot be null");
        this.rejectedDuplicates = registry.counter("consumed_outbox_events_total", "type", "rejected_duplicates");
        this.consumed = registry.counter("consumed_outbox_events_total", "type", "consumed");
        this.cleaned = registry.counter("consumed_outbox_events_total", "type", "cleaned");
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
    }

    @Override
    public boolean tryConsume(UUID id) {
        boolean isConsumed = delegate.tryConsume(id);
        if (isConsumed) {
            rejectedDuplicates.increment();
        } else {
            consumed.increment();
        }
        return isConsumed;
    }

    @Override
    public Set<UUID> tryConsumeAndGetDuplicates(Set<UUID> ids) {
        Set<UUID> alreadyConsumed = delegate.tryConsumeAndGetDuplicates(ids);
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
