package io.github.dmitriyiliyov.oncebox.starter.consumer;

import io.github.dmitriyiliyov.oncebox.core.consumer.cache.ConsumedOutboxCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class OutboxIdempotentConsumerCacheDecoratorSupplierUnitTests {

    @Test
    @DisplayName("UT constructor should throw NPE when cache is null")
    void constructor_shouldThrowNPE_whenCacheIsNull() {
        assertThatThrownBy(() -> new OutboxIdempotentConsumerCacheDecoratorSupplier(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("UT supply() should throw NPE when consumer is null")
    void supply_shouldThrowNPE_whenConsumerIsNull() {
        ConsumedOutboxCache cache = mock(ConsumedOutboxCache.class);
        OutboxIdempotentConsumerCacheDecoratorSupplier supplier = new OutboxIdempotentConsumerCacheDecoratorSupplier(cache);

        assertThatThrownBy(() -> supplier.supply(null))
                .isInstanceOf(NullPointerException.class);
    }
}
