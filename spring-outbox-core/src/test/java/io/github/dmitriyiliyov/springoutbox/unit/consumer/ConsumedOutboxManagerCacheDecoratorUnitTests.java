package io.github.dmitriyiliyov.springoutbox.unit.consumer;

import io.github.dmitriyiliyov.springoutbox.consumer.ConsumedOutboxManager;
import io.github.dmitriyiliyov.springoutbox.consumer.ConsumedOutboxManagerCacheDecorator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumedOutboxManagerCacheDecoratorUnitTests {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private ConsumedOutboxManager delegate;

    @Mock
    private MeterRegistry registry;

    @Mock
    private Counter hitsCounter;

    @Mock
    private Counter missesCounter;

    private ConsumedOutboxManagerCacheDecorator decorator;

    @Test
    @DisplayName("UT constructor when cacheName is null should throw NullPointerException")
    void constructor_whenCacheNameIsNull_shouldThrowNullPointerException() {
        // given
        String cacheName = null;

        // when + then
        assertThatThrownBy(() -> new ConsumedOutboxManagerCacheDecorator(
                cacheManager, cacheName, delegate, registry))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cacheName cannot be null");
    }

    @Test
    @DisplayName("UT constructor when cacheName is blank should throw IllegalArgumentException")
    void constructor_whenCacheNameIsBlank_shouldThrowIllegalArgumentException() {
        // given
        String cacheName = "   ";

        // when + then
        assertThatThrownBy(() -> new ConsumedOutboxManagerCacheDecorator(
                cacheManager, cacheName, delegate, registry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cacheName cannot be empty or blank");
    }

    @Test
    @DisplayName("UT constructor when cacheName is empty should throw IllegalArgumentException")
    void constructor_whenCacheNameIsEmpty_shouldThrowIllegalArgumentException() {
        // given
        String cacheName = "";

        // when + then
        assertThatThrownBy(() -> new ConsumedOutboxManagerCacheDecorator(
                cacheManager, cacheName, delegate, registry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cacheName cannot be empty or blank");
    }

    @Test
    @DisplayName("UT constructor when cache not found should throw IllegalStateException")
    void constructor_whenCacheNotFound_shouldThrowIllegalStateException() {
        // given
        String cacheName = "test-cache";
        when(cacheManager.getCache(cacheName)).thenReturn(null);

        // when + then
        assertThatThrownBy(() -> new ConsumedOutboxManagerCacheDecorator(
                cacheManager, cacheName, delegate, registry))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cache for outbox with name test-cache not found");
    }

    @Test
    @DisplayName("UT isConsumed() when cache hit should increment hits counter and return true")
    void isConsumed_whenCacheHit_shouldIncrementHitsCounterAndReturnTrue() {
        // given
        mockRegistry();
        UUID id = UUID.randomUUID();
        String cacheName = "test-cache";

        when(cacheManager.getCache(cacheName)).thenReturn(cache);
        when(cache.get(id.toString(), String.class)).thenReturn("");

        decorator = new ConsumedOutboxManagerCacheDecorator(cacheManager, cacheName, delegate, registry);

        // when
        boolean result = decorator.isConsumed(id);

        // then
        assertThat(result).isTrue();
        verify(cache).get(id.toString(), String.class);
        verify(hitsCounter).increment();
        verify(missesCounter, never()).increment();
        verify(delegate, never()).isConsumed(any());
    }

    @Test
    @DisplayName("UT isConsumed() when cache miss should increment misses counter and delegate")
    void isConsumed_whenCacheMiss_shouldIncrementMissesCounterAndDelegate() {
        // given
        mockRegistry();
        UUID id = UUID.randomUUID();
        String cacheName = "test-cache";
        when(cacheManager.getCache(cacheName)).thenReturn(cache);
        when(cache.get(id.toString(), String.class)).thenReturn(null);
        when(delegate.isConsumed(id)).thenReturn(true);

        decorator = new ConsumedOutboxManagerCacheDecorator(cacheManager, cacheName, delegate, registry);

        // when
        boolean result = decorator.isConsumed(id);

        // then
        assertThat(result).isTrue();
        verify(cache).get(id.toString(), String.class);
        verify(delegate).isConsumed(id);
        verify(cache).put(id.toString(), "");
        verify(missesCounter).increment();
        verify(hitsCounter, never()).increment();
    }

    @Test
    @DisplayName("UT isConsumed() when cache miss and delegate returns false should return false")
    void isConsumed_whenCacheMissAndDelegateReturnsFalse_shouldReturnFalse() {
        // given
        mockRegistry();
        UUID id = UUID.randomUUID();
        String cacheName = "test-cache";
        when(cacheManager.getCache(cacheName)).thenReturn(cache);
        when(cache.get(id.toString(), String.class)).thenReturn(null);
        when(delegate.isConsumed(id)).thenReturn(false);

        decorator = new ConsumedOutboxManagerCacheDecorator(cacheManager, cacheName, delegate, registry);

        // when
        boolean result = decorator.isConsumed(id);

        // then
        assertThat(result).isFalse();
        verify(delegate).isConsumed(id);
        verify(cache).put(id.toString(), "");
        verify(missesCounter).increment();
    }

    @Test
    @DisplayName("UT isConsumed() when cache becomes unavailable should delegate and increment misses")
    void isConsumed_whenCacheBecomesUnavailable_shouldDelegateAndIncrementMisses() {
        // given
        mockRegistry();
        UUID id = UUID.randomUUID();
        String cacheName = "test-cache";
        when(cacheManager.getCache(cacheName)).thenReturn(cache).thenReturn(null);
        when(delegate.isConsumed(id)).thenReturn(true);

        decorator = new ConsumedOutboxManagerCacheDecorator(cacheManager, cacheName, delegate, registry);

        // when
        boolean result = decorator.isConsumed(id);

        // then
        assertThat(result).isTrue();
        verify(delegate).isConsumed(id);
        verify(missesCounter).increment();
        verify(hitsCounter, never()).increment();
    }

    @Test
    @DisplayName("UT filterConsumed() should delegate to underlying manager")
    void filterConsumed_shouldDelegateToUnderlyingManager() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2);
        Set<UUID> expectedResult = Set.of(id1);
        String cacheName = "test-cache";
        when(cacheManager.getCache(cacheName)).thenReturn(cache);
        when(delegate.filterConsumed(ids)).thenReturn(expectedResult);

        decorator = new ConsumedOutboxManagerCacheDecorator(cacheManager, cacheName, delegate, registry);

        // when
        Set<UUID> result = decorator.filterConsumed(ids);

        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(delegate).filterConsumed(ids);
    }

    @Test
    @DisplayName("UT filterConsumed() with empty set should delegate")
    void filterConsumed_withEmptySet_shouldDelegate() {
        // given
        Set<UUID> ids = Set.of();
        Set<UUID> expectedResult = Set.of();
        String cacheName = "test-cache";
        when(cacheManager.getCache(cacheName)).thenReturn(cache);
        when(delegate.filterConsumed(ids)).thenReturn(expectedResult);

        decorator = new ConsumedOutboxManagerCacheDecorator(cacheManager, cacheName, delegate, registry);

        // when
        Set<UUID> result = decorator.filterConsumed(ids);

        // then
        assertThat(result).isEmpty();
        verify(delegate).filterConsumed(ids);
    }

    @Test
    @DisplayName("UT cleanBatchByTtl() should delegate to underlying manager")
    void cleanBatchByTtl_shouldDelegateToUnderlyingManager() {
        // given
        Duration ttl = Duration.ofHours(1);
        int batchSize = 100;
        int expectedResult = 50;
        String cacheName = "test-cache";
        when(cacheManager.getCache(cacheName)).thenReturn(cache);
        when(delegate.cleanBatchByTtl(ttl, batchSize)).thenReturn(expectedResult);

        decorator = new ConsumedOutboxManagerCacheDecorator(cacheManager, cacheName, delegate, registry);

        // when
        int result = decorator.cleanBatchByTtl(ttl, batchSize);

        // then
        assertThat(result).isEqualTo(50);
        verify(delegate).cleanBatchByTtl(ttl, batchSize);
    }

    @Test
    @DisplayName("UT cleanBatchByTtl() when no events cleaned should return zero")
    void cleanBatchByTtl_whenNoEventsCleaned_shouldReturnZero() {
        // given
        Duration ttl = Duration.ofHours(1);
        int batchSize = 100;
        String cacheName = "test-cache";
        when(cacheManager.getCache(cacheName)).thenReturn(cache);
        when(delegate.cleanBatchByTtl(ttl, batchSize)).thenReturn(0);

        decorator = new ConsumedOutboxManagerCacheDecorator(cacheManager, cacheName, delegate, registry);

        // when
        int result = decorator.cleanBatchByTtl(ttl, batchSize);

        // then
        assertThat(result).isZero();
        verify(delegate).cleanBatchByTtl(ttl, batchSize);
    }

    private void mockRegistry() {
        when(registry.counter(eq("consumed_outbox_events_total"), eq("type"), eq("cache-hit")))
                .thenReturn(hitsCounter);
        when(registry.counter(eq("consumed_outbox_events_total"), eq("type"), eq("cache-miss")))
                .thenReturn(missesCounter);
    }
}