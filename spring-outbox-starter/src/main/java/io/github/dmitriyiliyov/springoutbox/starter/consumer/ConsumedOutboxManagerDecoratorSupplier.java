package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxManager;

public interface ConsumedOutboxManagerDecoratorSupplier {
    ConsumedOutboxManager supply(ConsumedOutboxManager manager);
}
