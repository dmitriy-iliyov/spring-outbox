package io.github.dmitriyiliyov.springoutbox.core.consumer;

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

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConsumedOutboxCleanUpSchedulerUnitTests {

    @Mock
    OutboxPropertiesHolder.CleanUpPropertiesHolder properties;

    @Mock
    OutboxScheduleStrategy strategy;

    @Mock
    ConsumedOutboxManager manager;

    @InjectMocks
    ConsumedOutboxCleanUpScheduler tested;

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
    @DisplayName("UT schedule() when cleaned count equals batch size, continuable should return true")
    void schedule_whenCleanedCountEqualsBatchSize_continuableShouldReturnTrue() {
        // given
        int batchSize = 50;
        Duration ttl = Duration.ofDays(7);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getTtl()).thenReturn(ttl);
        when(manager.cleanBatchByTtl(ttl, batchSize)).thenReturn(batchSize);

        // when
        tested.schedule();
        boolean result = captureAndRun();

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("UT schedule() when cleaned count less than batch size, continuable should return false")
    void schedule_whenCleanedCountLessThanBatchSize_continuableShouldReturnFalse() {
        // given
        int batchSize = 50;
        Duration ttl = Duration.ofDays(7);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getTtl()).thenReturn(ttl);
        when(manager.cleanBatchByTtl(ttl, batchSize)).thenReturn(batchSize - 1);

        // when
        tested.schedule();
        boolean result = captureAndRun();

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("UT schedule() when cleaned count is zero, continuable should return false")
    void schedule_whenCleanedCountIsZero_continuableShouldReturnFalse() {
        // given
        int batchSize = 50;
        Duration ttl = Duration.ofDays(7);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getTtl()).thenReturn(ttl);
        when(manager.cleanBatchByTtl(ttl, batchSize)).thenReturn(0);

        // when
        tested.schedule();
        boolean result = captureAndRun();

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("UT schedule() should pass ttl and batchSize from properties to manager")
    void schedule_shouldPassCorrectParamsToManager() {
        // given
        int batchSize = 100;
        Duration ttl = Duration.ofHours(48);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getTtl()).thenReturn(ttl);
        when(manager.cleanBatchByTtl(any(), anyInt())).thenReturn(0);

        // when
        tested.schedule();
        captureAndRun();

        // then
        verify(manager).cleanBatchByTtl(ttl, batchSize);
    }

    @Test
    @DisplayName("UT schedule() when manager throws exception, continuable should return false and not rethrow")
    void schedule_whenManagerThrows_continuableShouldReturnFalseAndNotRethrow() {
        // given
        int batchSize = 50;
        Duration ttl = Duration.ofDays(7);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getTtl()).thenReturn(ttl);
        when(manager.cleanBatchByTtl(ttl, batchSize)).thenThrow(new RuntimeException("DB error"));

        // when
        tested.schedule();
        boolean result = assertDoesNotThrow(this::captureAndRun);

        // then
        assertFalse(result);
    }
}