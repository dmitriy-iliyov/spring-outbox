package io.github.dmitriyiliyov.springoutbox.core.publisher;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
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
class OutboxPublisherSchedulerUnitTests {

    @Mock
    private OutboxPublisherPropertiesHolder.EventPropertiesHolder eventProperties;

    @Mock
    private ScheduledExecutorService executor;

    @Mock
    private OutboxProcessor processor;

    @InjectMocks
    private OutboxPublisherScheduler scheduler;

    @Test
    @DisplayName("UT schedule() should schedule task with correct parameters")
    void schedule_shouldScheduleTaskWithCorrectParameters() {
        // given
        Duration initialDelay = Duration.ofSeconds(10);
        Duration fixedDelay = Duration.ofMinutes(1);
        when(eventProperties.getInitialDelay()).thenReturn(initialDelay);
        when(eventProperties.getFixedDelay()).thenReturn(fixedDelay);

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
    @DisplayName("UT schedule() task should call processor with correct parameters")
    void schedule_taskShouldCallProcessorWithCorrectParameters() {
        // given
        when(eventProperties.getInitialDelay()).thenReturn(Duration.ZERO);
        when(eventProperties.getFixedDelay()).thenReturn(Duration.ZERO);
        when(eventProperties.getEventType()).thenReturn("test-event");

        // when
        scheduler.schedule();

        // then
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).scheduleWithFixedDelay(captor.capture(), anyLong(), anyLong(), any());
        captor.getValue().run();

        verify(processor).process(eventProperties);
    }

    @Test
    @DisplayName("UT schedule() task should catch exception from processor")
    void schedule_taskShouldCatchExceptionFromProcessor() {
        // given
        when(eventProperties.getInitialDelay()).thenReturn(Duration.ZERO);
        when(eventProperties.getFixedDelay()).thenReturn(Duration.ZERO);
        when(eventProperties.getEventType()).thenReturn("test-event");
        doThrow(new RuntimeException("error")).when(processor).process(any());

        // when
        scheduler.schedule();

        // then
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).scheduleWithFixedDelay(captor.capture(), anyLong(), anyLong(), any());

        assertThatCode(() -> captor.getValue().run()).doesNotThrowAnyException();
    }
}
