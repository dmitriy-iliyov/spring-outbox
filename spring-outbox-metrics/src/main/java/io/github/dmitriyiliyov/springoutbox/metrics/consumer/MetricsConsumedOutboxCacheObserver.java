package io.github.dmitriyiliyov.springoutbox.metrics.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxCacheObserver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public class MetricsConsumedOutboxCacheObserver implements ConsumedOutboxCacheObserver {

    private final Counter hits;
    private final Counter misses;

    public MetricsConsumedOutboxCacheObserver(MeterRegistry registry) {
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
