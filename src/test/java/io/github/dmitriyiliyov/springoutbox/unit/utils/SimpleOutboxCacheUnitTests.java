package io.github.dmitriyiliyov.springoutbox.unit.utils;

import io.github.dmitriyiliyov.springoutbox.utils.SimpleOutboxCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleOutboxCacheUnitTests {

    enum TestStatus { PENDING, PROCESSED }

    @Test
    @DisplayName("UT getCountByStatus(), should return value within TTL")
    public void getCountByStatus_whenWithinTtl_shouldReturnValue() {
        // given
        SimpleOutboxCache<TestStatus> cache = new SimpleOutboxCache<>(1, 2, 2);
        cache.putCountByStatus(TestStatus.PENDING, 50L);

        // when
        Long result = cache.getCountByStatus(TestStatus.PENDING);

        // then
        assertEquals(50L, result);
    }

    @Test
    @DisplayName("UT getCountByStatus(), should return null after TTL expires")
    public void getCountByStatus_whenExpired_shouldReturnNull() throws InterruptedException {
        // given
        SimpleOutboxCache<TestStatus> cache = new SimpleOutboxCache<>(1, 1, 2);
        cache.putCountByStatus(TestStatus.PENDING, 50L);

        // when
        Thread.sleep(1100);
        Long result = cache.getCountByStatus(TestStatus.PENDING);

        // then
        assertNull(result);
    }

    @Test
    @DisplayName("UT getCountByEventTypeAndStatus(), should return value within TTL")
    public void getCountByEventTypeAndStatus_whenWithinTtl_shouldReturnValue() {
        // given
        SimpleOutboxCache<TestStatus> cache = new SimpleOutboxCache<>(1, 2, 2);
        cache.putCountByEventTypeAndStatus("evt", TestStatus.PENDING, 99L);

        // when
        Long result = cache.getCountByEventTypeAndStatus("evt", TestStatus.PENDING);

        // then
        assertEquals(99L, result);
    }

    @Test
    @DisplayName("UT getCountByEventTypeAndStatus(), should return null after TTL expires")
    public void getCountByEventTypeAndStatus_whenExpired_shouldReturnNull() throws InterruptedException {
        // given
        SimpleOutboxCache<TestStatus> cache = new SimpleOutboxCache<>(1, 2, 1);
        cache.putCountByEventTypeAndStatus("evt", TestStatus.PENDING, 99L);

        // when
        Thread.sleep(1100);
        Long result = cache.getCountByEventTypeAndStatus("evt", TestStatus.PENDING);

        // then
        assertNull(result);
    }

    @Test
    @DisplayName("UT putCountByStatus(), should update value and reset TTL")
    public void putCountByStatus_whenCalled_shouldUpdateValueAndResetTtl() throws InterruptedException {
        // given
        SimpleOutboxCache<TestStatus> cache = new SimpleOutboxCache<>(1, 1, 2);
        cache.putCountByStatus(TestStatus.PENDING, 10L);

        // when
        Thread.sleep(500);
        cache.putCountByStatus(TestStatus.PENDING, 20L); // обновляем значение
        Long result = cache.getCountByStatus(TestStatus.PENDING);

        // then
        assertEquals(20L, result);
    }

    @Test
    @DisplayName("UT putCountByEventTypeAndStatus(), should update value and reset TTL")
    public void putCountByEventTypeAndStatus_whenCalled_shouldUpdateValueAndResetTtl() throws InterruptedException {
        // given
        SimpleOutboxCache<TestStatus> cache = new SimpleOutboxCache<>(1, 2, 1);
        cache.putCountByEventTypeAndStatus("evt", TestStatus.PENDING, 5L);

        // when
        Thread.sleep(500);
        cache.putCountByEventTypeAndStatus("evt", TestStatus.PENDING, 15L); // обновляем значение
        Long result = cache.getCountByEventTypeAndStatus("evt", TestStatus.PENDING);

        // then
        assertEquals(15L, result);
    }
}
