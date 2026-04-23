package io.github.dmitriyiliyov.springoutbox.core.publisher;

import io.github.dmitriyiliyov.springoutbox.core.Continuable;
import io.github.dmitriyiliyov.springoutbox.core.OutboxPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduleStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OutboxCleanUpSchedulerUnitTests {

    @Mock
    OutboxPropertiesHolder.CleanUpPropertiesHolder properties;

    @Mock
    OutboxScheduleStrategy strategy;

    @Mock
    OutboxManager manager;

    @Mock
    Clock clock;

    @InjectMocks
    OutboxCleanUpScheduler tested;

    private boolean captureAndRun() {
        ArgumentCaptor<Continuable> captor = ArgumentCaptor.forClass(Continuable.class);
        verify(strategy).scheduleExecution(captor.capture());
        return captor.getValue().run();
    }

    @Test
    @DisplayName("UT schedule() should delegate execution to strategy")
    void schedule_shouldDelegateToStrategy() {
        // when
        tested.schedule();

        // then
        verify(strategy).scheduleExecution(any());
    }

    @Test
    @DisplayName("UT schedule() when deleted count equals batch size, continuable should return true")
    void schedule_whenDeletedCountEqualsBatchSize_continuableShouldReturnTrue() {
        // given
        int batchSize = 50;
        Instant now = Instant.parse("2024-01-01T12:00:00Z");
        Duration ttl = Duration.ofDays(7);

        when(clock.instant()).thenReturn(now);
        when(properties.getTtl()).thenReturn(ttl);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(manager.deleteProcessedBatch(now.minus(ttl), batchSize)).thenReturn(batchSize);

        // when
        tested.schedule();
        boolean result = captureAndRun();

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("UT schedule() when deleted count less than batch size, continuable should return false")
    void schedule_whenDeletedCountLessThanBatchSize_continuableShouldReturnFalse() {
        // given
        int batchSize = 50;
        Instant now = Instant.parse("2024-01-01T12:00:00Z");
        Duration ttl = Duration.ofDays(7);

        when(clock.instant()).thenReturn(now);
        when(properties.getTtl()).thenReturn(ttl);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(manager.deleteProcessedBatch(now.minus(ttl), batchSize)).thenReturn(batchSize - 1);

        // when
        tested.schedule();
        boolean result = captureAndRun();

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("UT schedule() when deleted count is zero, continuable should return false")
    void schedule_whenDeletedCountIsZero_continuableShouldReturnFalse() {
        // given
        int batchSize = 50;
        Instant now = Instant.parse("2024-01-01T12:00:00Z");
        Duration ttl = Duration.ofDays(7);

        when(clock.instant()).thenReturn(now);
        when(properties.getTtl()).thenReturn(ttl);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(manager.deleteProcessedBatch(now.minus(ttl), batchSize)).thenReturn(0);

        // when
        tested.schedule();
        boolean result = captureAndRun();

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("UT schedule() should pass threshold computed from clock and ttl to manager")
    void schedule_shouldPassCorrectThresholdToManager() {
        // given
        int batchSize = 100;
        Instant now = Instant.parse("2024-06-15T10:00:00Z");
        Duration ttl = Duration.ofHours(48);
        Instant expectedThreshold = now.minus(ttl);

        when(clock.instant()).thenReturn(now);
        when(properties.getTtl()).thenReturn(ttl);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(manager.deleteProcessedBatch(any(), anyInt())).thenReturn(0);

        // when
        tested.schedule();
        captureAndRun();

        // then
        verify(manager).deleteProcessedBatch(expectedThreshold, batchSize);
    }

    @Test
    @DisplayName("UT schedule() when manager throws exception, continuable should return false and not rethrow")
    void schedule_whenManagerThrows_continuableShouldReturnFalseAndNotRethrow() {
        // given
        Instant now = Instant.parse("2024-01-01T12:00:00Z");
        Duration ttl = Duration.ofDays(7);

        when(clock.instant()).thenReturn(now);
        when(properties.getTtl()).thenReturn(ttl);
        when(properties.getBatchSize()).thenReturn(50);
        when(manager.deleteProcessedBatch(any(), anyInt())).thenThrow(new RuntimeException("DB error"));

        // when
        tested.schedule();
        boolean result = assertDoesNotThrow(this::captureAndRun);

        // then
        assertFalse(result);
    }
}