package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.AbstractOutboxIdempotentConsumerDecorator;
import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxIdempotentConsumer;

public interface OutboxIdempotentConsumerDecoratorSupplier {
    AbstractOutboxIdempotentConsumerDecorator supply(OutboxIdempotentConsumer consumer);
}
