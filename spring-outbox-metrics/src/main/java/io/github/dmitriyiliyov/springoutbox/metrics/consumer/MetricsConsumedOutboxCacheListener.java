package io.github.dmitriyiliyov.springoutbox.metrics.consumer;

import io.github.dmitriyiliyov.springoutbox.consumer.cache.ConsumedOutboxCacheListener;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Objects;

public class MetricsConsumedOutboxCacheListener implements ConsumedOutboxCacheListener {

    private final Counter hits;
    private final Counter misses;

    public MetricsConsumedOutboxCacheListener(MeterRegistry registry) {
        Objects.requireNonNull(registry, "registry cannot be null");
        this.hits = registry.counter("consumed_outbox_cache_action_total", "action_type", "hit");
        this.misses = registry.counter("consumed_outbox_cache_action_total", "action_type", "miss");
    }

    @Override
    public void onHit() {
        hits.increment();
    }

    @Override
    public void onMiss() {
        misses.increment();
    }
}
