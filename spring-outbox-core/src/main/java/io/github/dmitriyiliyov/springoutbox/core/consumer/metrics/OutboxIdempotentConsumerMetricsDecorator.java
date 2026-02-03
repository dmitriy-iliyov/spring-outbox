package io.github.dmitriyiliyov.springoutbox.core.consumer.metrics;

import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxIdempotentConsumer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.List;
import java.util.function.Consumer;

public class OutboxIdempotentConsumerMetricsDecorator implements OutboxIdempotentConsumer {

    private final OutboxIdempotentConsumer delegate;
    private final Counter fails;

    public OutboxIdempotentConsumerMetricsDecorator(OutboxIdempotentConsumer delegate, MeterRegistry registry) {
        this.delegate = delegate;
        this.fails = registry.counter("consumed_outbox_events_total", "type", "failed");
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
