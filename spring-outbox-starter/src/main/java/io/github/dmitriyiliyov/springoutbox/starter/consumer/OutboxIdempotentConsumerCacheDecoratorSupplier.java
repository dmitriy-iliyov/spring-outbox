package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.consumer.cache.ConsumedOutboxCache;
import io.github.dmitriyiliyov.springoutbox.consumer.cache.OutboxIdempotentConsumerCacheDecorator;
import io.github.dmitriyiliyov.springoutbox.core.consumer.AbstractOutboxIdempotentConsumerDecorator;
import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxIdempotentConsumer;

import java.util.Objects;

public class OutboxIdempotentConsumerCacheDecoratorSupplier implements OutboxIdempotentConsumerDecoratorSupplier {

    private final ConsumedOutboxCache cache;

    public OutboxIdempotentConsumerCacheDecoratorSupplier(ConsumedOutboxCache cache) {
        this.cache = Objects.requireNonNull(cache, "cache cannot be null");
    }

    @Override
    public AbstractOutboxIdempotentConsumerDecorator supply(OutboxIdempotentConsumer consumer) {
        return new OutboxIdempotentConsumerCacheDecorator(
                Objects.requireNonNull(consumer, "consumer cannot be null"),
                cache
        );
    }
}
