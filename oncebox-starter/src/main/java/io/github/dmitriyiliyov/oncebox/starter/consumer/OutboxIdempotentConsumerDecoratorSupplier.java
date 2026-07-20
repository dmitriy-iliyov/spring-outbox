package io.github.dmitriyiliyov.oncebox.starter.consumer;

import io.github.dmitriyiliyov.oncebox.core.consumer.AbstractOutboxIdempotentConsumerDecorator;
import io.github.dmitriyiliyov.oncebox.core.consumer.OutboxIdempotentConsumer;

public interface OutboxIdempotentConsumerDecoratorSupplier {
    AbstractOutboxIdempotentConsumerDecorator supply(OutboxIdempotentConsumer consumer);
}
