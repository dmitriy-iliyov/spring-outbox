package io.github.dmitriyiliyov.springoutbox.metrics.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.AbstractOutboxIdempotentConsumerDecorator;
import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxEventIdExtractor;
import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxIdempotentConsumer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class OutboxIdempotentConsumerMetricsDecorator extends AbstractOutboxIdempotentConsumerDecorator {

    private final Counter fails;

    public OutboxIdempotentConsumerMetricsDecorator(MeterRegistry registry, OutboxIdempotentConsumer delegate) {
        super(Objects.requireNonNull(delegate, "delegate cannot be null"));
        Objects.requireNonNull(registry, "registry cannot be null");
        this.fails = registry.counter("consumed_outbox_events_total", "type", "failed");
    }

    @Override
    public void consume(UUID eventId, Runnable operation) {
        try {
            super.consume(eventId, operation);
        } catch (Exception e) {
            fails.increment();
            throw e;
        }
    }

    @Override
    public <T> void consume(T message, OutboxEventIdExtractor<T> idExtractor, Consumer<T> operation) {
        try {
            super.consume(message, idExtractor, operation);
        } catch (Exception e) {
            fails.increment();
            throw e;
        }
    }

    @Override
    public void consume(Set<UUID> ids, Consumer<Set<UUID>> operation) {
        try {
            super.consume(ids, operation);
        } catch (Exception e) {
            fails.increment(ids.size());
            throw e;
        }
    }

    @Override
    public <T> void consume(List<T> messages, OutboxEventIdExtractor<T> idExtractor, Consumer<List<T>> operation) {
        try {
            super.consume(messages, idExtractor, operation);
        } catch (Exception e) {
            fails.increment(messages.size());
            throw e;
        }
    }
}
