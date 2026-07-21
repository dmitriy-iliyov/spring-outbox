package io.github.dmitriyiliyov.oncebox.starter.consumer;

import io.github.dmitriyiliyov.oncebox.core.consumer.AbstractOutboxIdempotentConsumerDecorator;
import io.github.dmitriyiliyov.oncebox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.core.consumer.cache.ConsumedOutboxCache;
import io.github.dmitriyiliyov.oncebox.core.consumer.cache.OutboxIdempotentConsumerCacheDecorator;

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
