package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxDlqTransferSchedulerUnitTests {

    @Mock
    private OutboxPublisherPropertiesHolder.DlqPropertiesHolder properties;

    @Mock
    private ScheduledExecutorService executor;

    @Mock
    private OutboxDlqTransfer transfer;

    @InjectMocks
    private OutboxDlqTransferScheduler scheduler;

    @Test
    @DisplayName("UT schedule() should schedule two tasks with correct parameters")
    void schedule_shouldScheduleTwoTasksWithCorrectParameters() {
        // given
        Duration toInitialDelay = Duration.ofSeconds(10);
        Duration toFixedDelay = Duration.ofMinutes(1);
        Duration fromInitialDelay = Duration.ofSeconds(20);
        Duration fromFixedDelay = Duration.ofMinutes(2);

        when(properties.getTransferToInitialDelay()).thenReturn(toInitialDelay);
        when(properties.getTransferToFixedDelay()).thenReturn(toFixedDelay);
        when(properties.getTransferFromInitialDelay()).thenReturn(fromInitialDelay);
        when(properties.getTransferFromFixedDelay()).thenReturn(fromFixedDelay);

        // when
        scheduler.schedule();

        // then
        verify(executor).scheduleWithFixedDelay(
                any(Runnable.class),
                eq(10L),
                eq(60L),
                eq(TimeUnit.SECONDS)
        );
        verify(executor).scheduleWithFixedDelay(
                any(Runnable.class),
                eq(20L),
                eq(120L),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("UT schedule() tasks should call transfer with correct parameters")
    void schedule_tasksShouldCallTransferWithCorrectParameters() {
        // given
        int batchSize = 100;
        when(properties.getTransferToInitialDelay()).thenReturn(Duration.ZERO);
        when(properties.getTransferToFixedDelay()).thenReturn(Duration.ZERO);
        when(properties.getTransferFromInitialDelay()).thenReturn(Duration.ZERO);
        when(properties.getTransferFromFixedDelay()).thenReturn(Duration.ZERO);
        when(properties.getBatchSize()).thenReturn(batchSize);

        // when
        scheduler.schedule();

        // then
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor, times(2)).scheduleWithFixedDelay(captor.capture(), anyLong(), anyLong(), any());
        List<Runnable> tasks = captor.getAllValues();

        // Execute both tasks
        tasks.forEach(Runnable::run);

        verify(transfer).transferToDlq(batchSize);
        verify(transfer).transferFromDlq(batchSize);
    }

    @Test
    @DisplayName("UT schedule() tasks should catch exception from transfer")
    void schedule_tasksShouldCatchExceptionFromTransfer() {
        // given
        when(properties.getTransferToInitialDelay()).thenReturn(Duration.ZERO);
        when(properties.getTransferToFixedDelay()).thenReturn(Duration.ZERO);
        when(properties.getTransferFromInitialDelay()).thenReturn(Duration.ZERO);
        when(properties.getTransferFromFixedDelay()).thenReturn(Duration.ZERO);
        when(properties.getBatchSize()).thenReturn(1);
        doThrow(new RuntimeException("error")).when(transfer).transferToDlq(anyInt());
        doThrow(new RuntimeException("error")).when(transfer).transferFromDlq(anyInt());

        // when
        scheduler.schedule();

        // then
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor, times(2)).scheduleWithFixedDelay(captor.capture(), anyLong(), anyLong(), any());
        List<Runnable> tasks = captor.getAllValues();

        tasks.forEach(task ->
                assertThatCode(task::run).doesNotThrowAnyException()
        );
    }
}
