package io.github.dmitriyiliyov.springoutbox.core.consumer;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPropertiesHolder;
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumedOutboxCleanUpSchedulerUnitTests {

    @Mock
    private OutboxPropertiesHolder.CleanUpPropertiesHolder properties;

    @Mock
    private ScheduledExecutorService executor;

    @Mock
    private ConsumedOutboxManager manager;

    @InjectMocks
    private ConsumedOutboxCleanUpScheduler scheduler;

    @Test
    @DisplayName("UT schedule() should schedule task with correct parameters")
    void schedule_shouldScheduleTaskWithCorrectParameters() {
        // given
        Duration initialDelay = Duration.ofSeconds(10);
        Duration fixedDelay = Duration.ofMinutes(1);
        when(properties.getInitialDelay()).thenReturn(initialDelay);
        when(properties.getFixedDelay()).thenReturn(fixedDelay);

        // when
        scheduler.schedule();

        // then
        verify(executor).scheduleWithFixedDelay(
                any(Runnable.class),
                eq(10L),
                eq(60L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("UT schedule() task should call manager with correct parameters")
    void schedule_taskShouldCallManagerWithCorrectParameters() {
        // given
        Duration ttl = Duration.ofHours(24);
        int batchSize = 100;
        when(properties.getInitialDelay()).thenReturn(Duration.ZERO);
        when(properties.getFixedDelay()).thenReturn(Duration.ZERO);
        when(properties.getTtl()).thenReturn(ttl);
        when(properties.getBatchSize()).thenReturn(batchSize);

        // when
        scheduler.schedule();

        // then
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).scheduleWithFixedDelay(captor.capture(), anyLong(), anyLong(), any());
        captor.getValue().run();

        verify(manager).cleanBatchByTtl(ttl, batchSize);
    }

    @Test
    @DisplayName("UT schedule() task should catch exception from manager")
    void schedule_taskShouldCatchExceptionFromManager() {
        // given
        when(properties.getInitialDelay()).thenReturn(Duration.ZERO);
        when(properties.getFixedDelay()).thenReturn(Duration.ZERO);
        when(properties.getTtl()).thenReturn(Duration.ZERO);
        when(properties.getBatchSize()).thenReturn(1);
        doThrow(new RuntimeException("error")).when(manager).cleanBatchByTtl(any(), anyInt());

        // when
        scheduler.schedule();

        // then
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).scheduleWithFixedDelay(captor.capture(), anyLong(), anyLong(), any());

        assertThatCode(() -> captor.getValue().run()).doesNotThrowAnyException();
    }
}
