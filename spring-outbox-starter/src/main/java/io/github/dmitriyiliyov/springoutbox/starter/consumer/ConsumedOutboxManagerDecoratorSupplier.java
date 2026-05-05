package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxManager;

/**
 * A supplier for decorating {@link ConsumedOutboxManager} instances.
 */
public interface ConsumedOutboxManagerDecoratorSupplier {
    
    /**
     * Decorates and supplies a {@link ConsumedOutboxManager}.
     *
     * @param manager the base {@link ConsumedOutboxManager} to be decorated.
     * @return the decorated {@link ConsumedOutboxManager}.
     */
    ConsumedOutboxManager supply(ConsumedOutboxManager manager);
}
