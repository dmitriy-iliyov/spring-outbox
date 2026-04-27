package io.github.dmitriyiliyov.springoutbox.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdaptiveOutboxScheduleStrategyUnitTests {

    @Mock
    private AdaptivePollingPropertiesHolder properties;

    @Mock
    private ScheduledExecutorService executor;

    @Mock
    private OutboxScheduleStrategyListener listener;

    @Mock
    private ContinuableTask task;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    private AdaptiveOutboxScheduleStrategy strategy;

    private static final long MIN_DELAY     = 100L;
    private static final long MAX_DELAY     = 3200L;
    private static final long INITIAL_DELAY = 0L;
    private static final double MULTIPLIER  = 2.0;

    @BeforeEach
    void setUp() {
        when(properties.getMinFixedDelay()).thenReturn(Duration.ofMillis(MIN_DELAY));
        when(properties.getMaxFixedDelay()).thenReturn(Duration.ofMillis(MAX_DELAY));
        when(properties.getInitialDelay()).thenReturn(Duration.ofMillis(INITIAL_DELAY));
        when(properties.getMultiplier()).thenReturn(MULTIPLIER);
        strategy = new AdaptiveOutboxScheduleStrategy(properties, executor, listener);
    }

    @Test
    void scheduleExecution_schedulesWithInitialDelay() {
        doReturn(scheduledFuture).when(executor).schedule(any(Runnable.class), anyLong(), any());

        strategy.scheduleExecution(task);

        verify(executor).schedule(any(Runnable.class), eq(INITIAL_DELAY), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void scheduleExecution_doesNotExecuteTaskImmediately() {
        doReturn(scheduledFuture).when(executor).schedule(any(Runnable.class), anyLong(), any());

        strategy.scheduleExecution(task);

        verifyNoInteractions(task);
    }

    @Test
    void afterEmptyPoll_delayIncreasedByMultiplier() throws Exception {
        doReturn(scheduledFuture).when(executor).schedule(any(Runnable.class), anyLong(), any());
        when(task.run()).thenReturn(false);

        strategy.scheduleExecution(task);
        runCapturedTask(0);

        ArgumentCaptor<Long> delays = captureDelays(2);
        assertThat(delays.getAllValues().get(0)).isEqualTo(INITIAL_DELAY);
        assertThat(delays.getAllValues().get(1)).isEqualTo((long) (MIN_DELAY * MULTIPLIER));
    }

    @Test
    void delayDoesNotExceedMaxFixedDelay() throws Exception {
        doReturn(scheduledFuture).when(executor).schedule(any(Runnable.class), anyLong(), any());
        when(task.run()).thenReturn(false);

        strategy.scheduleExecution(task);
        for (int i = 0; i < 7; i++) {
            runCapturedTask(i);
        }

        ArgumentCaptor<Long> delays = captureDelays(8);
        assertThat(delays.getAllValues().get(7)).isEqualTo(MAX_DELAY);
    }

    @Test
    void afterSuccessfulPoll_delayResetsToMin() throws Exception {
        doReturn(scheduledFuture).when(executor).schedule(any(Runnable.class), anyLong(), any());
        when(task.run()).thenReturn(false, false, true);

        strategy.scheduleExecution(task);
        runCapturedTask(0);
        runCapturedTask(1);
        runCapturedTask(2);

        ArgumentCaptor<Long> delays = captureDelays(4);
        assertThat(delays.getAllValues().get(3)).isEqualTo(MIN_DELAY);
    }

    @Test
    void scheduleNext_calledTwice_onlyOneTaskScheduled() {
        doReturn(scheduledFuture).when(executor).schedule(any(Runnable.class), anyLong(), any());

        strategy.scheduleExecution(task);
        strategy.scheduleExecution(task);

        verify(executor, times(1)).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    void taskThrowsException_nextIterationStillScheduled() throws Exception {
        doReturn(scheduledFuture).when(executor).schedule(any(Runnable.class), anyLong(), any());
        when(task.run()).thenThrow(new RuntimeException("boom"));

        strategy.scheduleExecution(task);
        runCapturedTask(0);

        verify(executor, times(2)).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    void taskThrowsException_delayIncreases() throws Exception {
        doReturn(scheduledFuture).when(executor).schedule(any(Runnable.class), anyLong(), any());
        when(task.run()).thenThrow(new RuntimeException("boom"));

        strategy.scheduleExecution(task);
        runCapturedTask(0);

        ArgumentCaptor<Long> delays = captureDelays(2);
        assertThat(delays.getAllValues().get(1)).isEqualTo((long) (MIN_DELAY * MULTIPLIER));
    }

    @Test
    void whenExecutorShutdownBeforeSchedule_nothingScheduled() {
        when(executor.isShutdown()).thenReturn(true);

        strategy.scheduleExecution(task);

        verify(executor, never()).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    void whenExecutorShutdownAfterRun_nextScheduleSkipped() throws Exception {
        doReturn(scheduledFuture).when(executor).schedule(any(Runnable.class), anyLong(), any());
        when(task.run()).thenReturn(false);
        when(executor.isShutdown()).thenReturn(false, false, true);

        strategy.scheduleExecution(task);
        runCapturedTask(0);

        verify(executor, times(1)).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    void whenScheduleRejected_taskInFlightReleasedAndCanBeRescheduled() throws Exception {
        when(task.run()).thenReturn(false);
        doReturn(scheduledFuture)
                .doThrow(new RejectedExecutionException("shutdown"))
                .when(executor).schedule(any(Runnable.class), anyLong(), any());

        strategy.scheduleExecution(task);
        runCapturedTask(0);

        doReturn(scheduledFuture).when(executor).schedule(any(Runnable.class), anyLong(), any());
        strategy.scheduleExecution(task);

        verify(executor, times(3)).schedule(any(Runnable.class), anyLong(), any());
    }

    private void runCapturedTask(int callIndex) {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor, atLeast(callIndex + 1)).schedule(captor.capture(), anyLong(), any());
        captor.getAllValues().get(callIndex).run();
    }

    private ArgumentCaptor<Long> captureDelays(int expectedCalls) {
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(executor, times(expectedCalls)).schedule(any(Runnable.class), captor.capture(), any());
        return captor;
    }
}