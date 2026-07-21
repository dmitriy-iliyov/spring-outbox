package io.github.dmitriyiliyov.oncebox.core.consumer.cache;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class ConsumedOutboxCacheNoopUnitTests {

    @Test
    @DisplayName("UT isConsumed() on NOOP_CACHE should always return false")
    void isConsumed_onNoopCache_shouldReturnFalse() {
        // given
        UUID id = UUID.randomUUID();
        ConsumedOutboxCache noopCache = ConsumedOutboxCache.NOOP_CACHE;

        // when
        boolean result = noopCache.isConsumed(id);

        // then
        Assertions.assertThat(result).isFalse();
    }

    @Test
    @DisplayName("UT consume() on NOOP_CACHE should not throw any exception")
    void consume_onNoopCache_shouldNotThrowException() {
        // given
        UUID id = UUID.randomUUID();
        ConsumedOutboxCache noopCache = ConsumedOutboxCache.NOOP_CACHE;

        // when + then
        Assertions.assertThatCode(() -> noopCache.consume(id))
                .doesNotThrowAnyException();
    }
}