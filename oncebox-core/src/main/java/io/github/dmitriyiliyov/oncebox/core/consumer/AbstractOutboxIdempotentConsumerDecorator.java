package io.github.dmitriyiliyov.oncebox.core.consumer;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class AbstractOutboxIdempotentConsumerDecorator implements OutboxIdempotentConsumer {

    protected final OutboxIdempotentConsumer delegate;

    public AbstractOutboxIdempotentConsumerDecorator(OutboxIdempotentConsumer delegate) {
        this.delegate = delegate;
    }

    @Override
    public void consume(UUID eventId, Runnable operation) {
        delegate.consume(eventId, operation);
    }

    @Override
    public <T> void consume(T message, OutboxEventIdExtractor<T> idExtractor, Consumer<T> operation) {
        delegate.consume(message, idExtractor, operation);
    }

    @Override
    public void consume(Set<UUID> ids, Consumer<Set<UUID>> operation) {
        delegate.consume(ids, operation);
    }

    @Override
    public <T> void consume(List<T> messages, OutboxEventIdExtractor<T> idExtractor, Consumer<List<T>> operation) {
        delegate.consume(messages, idExtractor, operation);
    }
}
