package io.github.dmitriyiliyov.springoutbox.core.consumer;

import io.github.dmitriyiliyov.springoutbox.core.it.BaseRedisIntegrationTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class ConsumedOutboxManagerCacheDecoratorIntegrationTests extends BaseRedisIntegrationTests {

    private final CacheManager cacheManager;

    @Mock
    private ConsumedOutboxManager delegate;

    @Mock
    private ConsumedOutboxCacheObserver cacheObserver;

    private ConsumedOutboxManagerCacheDecorator decorator;

    private static final String CACHE_NAME = "consumed-outbox";

    ConsumedOutboxManagerCacheDecoratorIntegrationTests(@Qualifier("redisIntegrationTestsCacheManager") CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @BeforeEach
    void setUp() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }
        decorator = new ConsumedOutboxManagerCacheDecorator(
                cacheManager, CACHE_NAME, delegate, cacheObserver
        );
    }

    @Test
    @DisplayName("IT constructor throws when cacheName is null")
    void constructor_nullCacheName_throwsNullPointerException() {
        assertThatThrownBy(() ->
                new ConsumedOutboxManagerCacheDecorator(cacheManager, null, delegate, cacheObserver)
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("IT constructor throws when cacheName is blank")
    void constructor_blankCacheName_throwsIllegalArgumentException() {
        assertThatThrownBy(() ->
                new ConsumedOutboxManagerCacheDecorator(cacheManager, "  ", delegate, cacheObserver)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("IT isConsumed() cache miss - calls delegate and puts into cache")
    void isConsumed_cacheMiss_callsDelegateAndCaches() {
        UUID id = UUID.randomUUID();
        when(delegate.isConsumed(id)).thenReturn(false);

        boolean result = decorator.isConsumed(id);

        assertThat(result).isFalse();
        verify(delegate, times(1)).isConsumed(id);
        verify(cacheObserver, times(1)).onMiss();
        verify(cacheObserver, never()).onHit();
        verifyNoMoreInteractions(delegate, cacheObserver);
    }

    @Test
    @DisplayName("IT isConsumed() cache hit - does not call delegate")
    void isConsumed_cacheHit_doesNotCallDelegate() {
        UUID id = UUID.randomUUID();
        when(delegate.isConsumed(id)).thenReturn(true);
        decorator.isConsumed(id);

        boolean result = decorator.isConsumed(id);

        assertThat(result).isTrue();
        verify(delegate, times(1)).isConsumed(id);
        verify(cacheObserver, times(1)).onMiss();
        verify(cacheObserver, times(1)).onHit();
        verifyNoMoreInteractions(delegate, cacheObserver);
    }

    @Test
    @DisplayName("IT isConsumed() delegate returns true - result is true and cached")
    void isConsumed_delegateReturnsTrue_resultIsTrueAndCached() {
        UUID id = UUID.randomUUID();
        when(delegate.isConsumed(id)).thenReturn(true);

        boolean first  = decorator.isConsumed(id);
        boolean second = decorator.isConsumed(id);

        assertThat(first).isTrue();
        assertThat(second).isTrue();
        verify(delegate, times(1)).isConsumed(id);
    }

    @Test
    @DisplayName("IT isConsumed() different ids cached independently")
    void isConsumed_differentIds_cachedIndependently() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(delegate.isConsumed(id1)).thenReturn(false);
        when(delegate.isConsumed(id2)).thenReturn(true);

        decorator.isConsumed(id1);
        decorator.isConsumed(id2);
        decorator.isConsumed(id1);
        decorator.isConsumed(id2);

        verify(delegate, times(1)).isConsumed(id1);
        verify(delegate, times(1)).isConsumed(id2);
        verify(cacheObserver, times(2)).onMiss();
        verify(cacheObserver, times(2)).onHit();
    }

    @Test
    @DisplayName("IT isConsumed() after cache clear - calls delegate again")
    void isConsumed_afterCacheClear_callsDelegateAgain() {
        UUID id = UUID.randomUUID();
        when(delegate.isConsumed(id)).thenReturn(false);
        decorator.isConsumed(id);

        cacheManager.getCache(CACHE_NAME).clear();
        decorator.isConsumed(id);

        verify(delegate, times(2)).isConsumed(id);
        verify(cacheObserver, times(2)).onMiss();
    }

    @Test
    @DisplayName("IT filterConsumed() delegates to underlying manager without caching")
    void filterConsumed_delegatesToUnderlyingManager() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2);
        when(delegate.filterConsumed(ids)).thenReturn(Set.of(id1));

        Set<UUID> result = decorator.filterConsumed(ids);

        assertThat(result).containsOnly(id1);
        verify(delegate, times(1)).filterConsumed(ids);
    }

    @Test
    @DisplayName("IT filterConsumed() called twice - calls delegate both times (no caching)")
    void filterConsumed_calledTwice_callsDelegateBothTimes() {
        Set<UUID> ids = Set.of(UUID.randomUUID());
        when(delegate.filterConsumed(ids)).thenReturn(Set.of());

        decorator.filterConsumed(ids);
        decorator.filterConsumed(ids);

        verify(delegate, times(2)).filterConsumed(ids);
    }

    @Test
    @DisplayName("IT cleanBatchByTtl() delegates to underlying manager")
    void cleanBatchByTtl_delegatesToUnderlyingManager() {
        Duration ttl = Duration.ofDays(7);
        when(delegate.cleanBatchByTtl(ttl, 100)).thenReturn(5);

        int result = decorator.cleanBatchByTtl(ttl, 100);

        assertThat(result).isEqualTo(5);
        verify(delegate, times(1)).cleanBatchByTtl(ttl, 100);
    }

    @Test
    @DisplayName("IT cleanBatchByTtl() returns zero when nothing to clean")
    void cleanBatchByTtl_nothingToClean_returnsZero() {
        when(delegate.cleanBatchByTtl(any(), anyInt())).thenReturn(0);

        assertThat(decorator.cleanBatchByTtl(Duration.ofDays(1), 50)).isEqualTo(0);
    }
}