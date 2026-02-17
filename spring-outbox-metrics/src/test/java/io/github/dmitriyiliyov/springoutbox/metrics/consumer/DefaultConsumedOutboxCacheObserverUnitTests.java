package io.github.dmitriyiliyov.springoutbox.metrics.consumer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultConsumedOutboxCacheObserverUnitTests {

    @Mock
    MeterRegistry registry;

    @Mock
    Counter hitsCounter;

    @Mock
    Counter missesCounter;

    DefaultConsumedOutboxCacheObserver observer;

    @BeforeEach
    void setUp() {
        when(registry.counter(anyString(), eq("type"), eq("cache-hit"))).thenReturn(hitsCounter);
        when(registry.counter(anyString(), eq("type"), eq("cache-miss"))).thenReturn(missesCounter);
        observer = new DefaultConsumedOutboxCacheObserver(registry);
    }

    @Test
    @DisplayName("UT onHit() should increment hits counter")
    void onHit_shouldIncrementHitsCounter() {
        // when
        observer.onHit();

        // then
        verify(hitsCounter).increment();
    }

    @Test
    @DisplayName("UT onMiss() should increment misses counter")
    void onMiss_shouldIncrementMissesCounter() {
        // when
        observer.onMiss();

        // then
        verify(missesCounter).increment();
    }
}
