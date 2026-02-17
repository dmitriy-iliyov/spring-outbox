package io.github.dmitriyiliyov.springoutbox.metrics.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxCacheObserver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public class DefaultConsumedOutboxCacheObserver implements ConsumedOutboxCacheObserver {

    private final Counter hits;
    private final Counter misses;

    public DefaultConsumedOutboxCacheObserver(MeterRegistry registry) {
        this.hits = registry.counter("consumed_outbox_events_total", "type", "cache-hit");
        this.misses = registry.counter("consumed_outbox_events_total", "type", "cache-miss");
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
