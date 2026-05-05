package io.github.dmitriyiliyov.springoutbox.core.polling;

import io.github.dmitriyiliyov.springoutbox.core.ContinuableTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FixedOutboxScheduleStrategyUnitTests {

    @Mock
    FixedPollingPropertiesHolder properties;

    @Mock
    ScheduledExecutorService executor;

    @Mock
    ContinuableTask task;

    @Mock
    OutboxScheduleStrategyListener listener;

    @InjectMocks
    FixedOutboxScheduleStrategy tested;

    private Runnable captureScheduledRunnable() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).scheduleWithFixedDelay(captor.capture(), anyLong(), anyLong(), any());
        return captor.getValue();
    }

    @Test
    @DisplayName("UT scheduleExecution() should call scheduleWithFixedDelay on executor")
    void scheduleExecution_shouldCallScheduleWithFixedDelay() {
        // given
        when(properties.getInitialDelay()).thenReturn(Duration.ofMillis(100));
        when(properties.getFixedDelay()).thenReturn(Duration.ofMillis(500));

        // when
        tested.scheduleExecution(task);

        // then
        verify(executor).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        verify(listener).onDelayChanged(nullable(Long.class));
    }

    @Test
    @DisplayName("UT scheduleExecution() should pass initialDelay and fixedDelay in milliseconds to executor")
    void scheduleExecution_shouldPassCorrectDelaysToExecutor() {
        // given
        Duration initialDelay = Duration.ofSeconds(2);
        Duration fixedDelay = Duration.ofSeconds(5);
        when(properties.getInitialDelay()).thenReturn(initialDelay);
        when(properties.getFixedDelay()).thenReturn(fixedDelay);

        // when
        tested.scheduleExecution(task);

        // then
        verify(executor).scheduleWithFixedDelay(
                any(Runnable.class),
                eq(initialDelay.toMillis()),
                eq(fixedDelay.toMillis()),
                eq(TimeUnit.MILLISECONDS)
        );
        verify(listener).onDelayChanged(nullable(Long.class));
    }

    @Test
    @DisplayName("UT scheduleExecution() when runnable is invoked, should call task.run()")
    void scheduleExecution_whenRunnableInvoked_shouldCallTaskRun() {
        // given
        when(properties.getInitialDelay()).thenReturn(Duration.ofMillis(0));
        when(properties.getFixedDelay()).thenReturn(Duration.ofMillis(500));

        // when
        tested.scheduleExecution(task);
        captureScheduledRunnable().run();

        // then
        verify(task).run();
        verify(listener).onDelayChanged(nullable(Long.class));
    }

    @Test
    @DisplayName("UT scheduleExecution() when task throws exception, runnable should not rethrow")
    void scheduleExecution_whenTaskThrows_runnableShouldNotRethrow() {
        // given
        when(properties.getInitialDelay()).thenReturn(Duration.ofMillis(0));
        when(properties.getFixedDelay()).thenReturn(Duration.ofMillis(500));
        when(task.run()).thenThrow(new RuntimeException("task error"));

        // when
        tested.scheduleExecution(task);
        Runnable runnable = captureScheduledRunnable();

        // then
        assertDoesNotThrow(runnable::run);
        verify(listener).onDelayChanged(nullable(Long.class));
    }

    @Test
    @DisplayName("UT scheduleExecution() when task throws Error, runnable should not rethrow")
    void scheduleExecution_whenTaskThrowsError_runnableShouldNotRethrow() {
        // given
        when(properties.getInitialDelay()).thenReturn(Duration.ofMillis(0));
        when(properties.getFixedDelay()).thenReturn(Duration.ofMillis(500));
        when(task.run()).thenThrow(new Error("fatal error"));

        // when
        tested.scheduleExecution(task);
        Runnable runnable = captureScheduledRunnable();

        // then
        assertDoesNotThrow(runnable::run);
        verify(listener).onDelayChanged(nullable(Long.class));
    }

    @Test
    @DisplayName("UT scheduleExecution() should use MILLISECONDS as time unit")
    void scheduleExecution_shouldUseMillisecondsTimeUnit() {
        // given
        when(properties.getInitialDelay()).thenReturn(Duration.ofMillis(100));
        when(properties.getFixedDelay()).thenReturn(Duration.ofMillis(500));

        // when
        tested.scheduleExecution(task);

        // then
        verify(executor).scheduleWithFixedDelay(any(), anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS));
        verify(listener).onDelayChanged(nullable(Long.class));
    }
}