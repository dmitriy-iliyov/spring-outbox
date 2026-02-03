package io.github.dmitriyiliyov.springoutbox.core.publisher.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class CacheHelperUnitTests {

    enum TestStatus { PENDING, PROCESSED }

    @Test
    @DisplayName("UT count(), should return cached value if present")
    public void count_shouldReturnCachedValueIfPresent() {
        // given
        OutboxCache<TestStatus> cache = mock(OutboxCache.class);
        when(cache.getCount()).thenReturn(5L);

        // when
        long result = CacheHelper.count(cache, () -> 10L);

        // then
        assertEquals(5L, result);
        verify(cache, never()).putCount(anyLong());
    }

    @Test
    @DisplayName("UT count() when cache has not, should compute and put value if not cached")
    public void count_shouldComputeAndPutIfNotCached() {
        // given
        OutboxCache<TestStatus> cache = mock(OutboxCache.class);
        when(cache.getCount()).thenReturn(null);
        when(cache.putCount(10L)).thenReturn(10L);

        // when
        long result = CacheHelper.count(cache, () -> 10L);

        // then
        assertEquals(10L, result);
        verify(cache).putCount(10L);
    }

    @Test
    @DisplayName("UT countByStatus(), should return cached value if present")
    public void countByStatus_shouldReturnCachedValueIfPresent() {
        // given
        OutboxCache<TestStatus> cache = mock(OutboxCache.class);
        when(cache.getCountByStatus(TestStatus.PENDING)).thenReturn(3L);

        // when
        long result = CacheHelper.countByStatus(cache, TestStatus.PENDING, s -> 10L);

        // then
        assertEquals(3L, result);
        verify(cache, never()).putCountByStatus(any(), anyLong());
    }

    @Test
    @DisplayName("UT countByStatus() when cache has not, should compute and put value if not cached")
    public void countByStatus_shouldComputeAndPutIfNotCached() {
        // given
        OutboxCache<TestStatus> cache = mock(OutboxCache.class);
        when(cache.getCountByStatus(TestStatus.PENDING)).thenReturn(null);
        when(cache.putCountByStatus(TestStatus.PENDING, 10L)).thenReturn(10L);

        // when
        long result = CacheHelper.countByStatus(cache, TestStatus.PENDING, s -> 10L);

        // then
        assertEquals(10L, result);
        verify(cache).putCountByStatus(TestStatus.PENDING, 10L);
    }

    @Test
    @DisplayName("UT countByEventTypeAndStatus(), should return cached value if present")
    public void countByEventTypeAndStatus_shouldReturnCachedValueIfPresent() {
        // given
        OutboxCache<TestStatus> cache = mock(OutboxCache.class);
        when(cache.getCountByEventTypeAndStatus("evt", TestStatus.PENDING)).thenReturn(7L);

        // when
        long result = CacheHelper.countByEventTypeAndStatus(cache, "evt", TestStatus.PENDING, (e,s) -> 10L);

        // then
        assertEquals(7L, result);
        verify(cache, never()).putCountByEventTypeAndStatus(anyString(), any(), anyLong());
    }

    @Test
    @DisplayName("UT countByEventTypeAndStatus() when cache has not, should compute and put value if not cached")
    public void countByEventTypeAndStatus_shouldComputeAndPutIfNotCached() {
        // given
        OutboxCache<TestStatus> cache = mock(OutboxCache.class);
        when(cache.getCountByEventTypeAndStatus("evt", TestStatus.PENDING)).thenReturn(null);
        when(cache.putCountByEventTypeAndStatus("evt", TestStatus.PENDING, 10L)).thenReturn(10L);

        // when
        long result = CacheHelper.countByEventTypeAndStatus(cache, "evt", TestStatus.PENDING, (e,s) -> 10L);

        // then
        assertEquals(10L, result);
        verify(cache).putCountByEventTypeAndStatus("evt", TestStatus.PENDING, 10L);
    }
}
