package io.github.dmitriyiliyov.springoutbox.consumer.cache;

import io.github.dmitriyiliyov.springoutbox.core.consumer.AbstractOutboxIdempotentConsumerDecorator;
import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxEventIdExtractor;
import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxIdempotentConsumer;

import java.util.UUID;
import java.util.function.Consumer;

public class OutboxIdempotentConsumerCacheDecorator extends AbstractOutboxIdempotentConsumerDecorator {

    private final ConsumedOutboxCache cache;

    public OutboxIdempotentConsumerCacheDecorator(OutboxIdempotentConsumer delegate, ConsumedOutboxCache cache) {
        super(delegate);
        this.cache = cache;
    }

    @Override
    public void consume(UUID eventId, Runnable operation) {
        if (cache.isConsumed(eventId)) {
            return;
        }
        super.consume(eventId, operation);
        cache.consume(eventId);
    }

    @Override
    public <T> void consume(T message, OutboxEventIdExtractor<T> idExtractor, Consumer<T> operation) {
        UUID eventId = idExtractor.extract(message);
        if (cache.isConsumed(eventId)) {
            return;
        }
        super.consume(message, idExtractor, operation);
        cache.consume(eventId);
    }
}
