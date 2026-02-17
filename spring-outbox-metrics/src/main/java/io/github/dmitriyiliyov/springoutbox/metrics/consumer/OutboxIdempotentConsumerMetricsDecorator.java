package io.github.dmitriyiliyov.springoutbox.metrics.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxIdempotentConsumer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.List;
import java.util.function.Consumer;

public class OutboxIdempotentConsumerMetricsDecorator implements OutboxIdempotentConsumer {

    private final Counter fails;
    private final OutboxIdempotentConsumer delegate;

    public OutboxIdempotentConsumerMetricsDecorator(MeterRegistry registry, OutboxIdempotentConsumer delegate) {
        this.fails = registry.counter("consumed_outbox_events_total", "type", "failed");
        this.delegate = delegate;
    }

    @Override
    public <T> void consume(T message, Runnable operation) {
        try {
            delegate.consume(message, operation);
        } catch (Exception e) {
            fails.increment();
            throw e;
        }
    }

    @Override
    public <T> void consume(List<T> messages, Consumer<List<T>> operation) {
        try {
            delegate.consume(messages, operation);
        } catch (Exception e) {
            fails.increment(messages.size());
            throw e;
        }
    }
}
