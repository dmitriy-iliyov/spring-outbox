package io.github.dmitriyiliyov.springoutbox.unit.config;

import io.github.dmitriyiliyov.springoutbox.config.OutboxAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxAutoConfigurationUnitTests {

    @InjectMocks
    OutboxAutoConfiguration tested;

    @Test
    @DisplayName("UT onDestroy() should shutdown executor gracefully")
    void onDestroy_shouldShutdownGracefully() throws InterruptedException {
        // given
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        OutboxAutoConfiguration spyTested = spy(tested);
        doReturn(executor).when(spyTested).outboxScheduledExecutorService();
        when(executor.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);

        // when
        spyTested.onDestroy();

        // then
        verify(executor).shutdown();
        verify(executor).awaitTermination(30, TimeUnit.SECONDS);
        verify(executor, never()).shutdownNow();
    }

    @Test
    @DisplayName("UT onDestroy() when awaitTermination fails first time should shutdownNow")
    void onDestroy_whenAwaitTerminationFailsFirstTime_shouldShutdownNow() throws InterruptedException {
        // given
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        OutboxAutoConfiguration spyTested = spy(tested);
        doReturn(executor).when(spyTested).outboxScheduledExecutorService();
        when(executor.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenReturn(false)
                .thenReturn(true);

        // when
        spyTested.onDestroy();

        // then
        verify(executor).shutdown();
        verify(executor, times(2)).awaitTermination(30, TimeUnit.SECONDS);
        verify(executor).shutdownNow();
    }

    @Test
    @DisplayName("UT onDestroy() when awaitTermination fails both times should log error")
    void onDestroy_whenAwaitTerminationFailsBothTimes_shouldLogError() throws InterruptedException {
        // given
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        OutboxAutoConfiguration spyTested = spy(tested);
        doReturn(executor).when(spyTested).outboxScheduledExecutorService();
        when(executor.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenReturn(false)
                .thenReturn(false);

        // when
        spyTested.onDestroy();

        // then
        verify(executor).shutdown();
        verify(executor, times(2)).awaitTermination(30, TimeUnit.SECONDS);
        verify(executor).shutdownNow();
    }

    @Test
    @DisplayName("UT onDestroy() when InterruptedException occurs should shutdownNow and interrupt thread")
    void onDestroy_whenInterruptedException_shouldShutdownNowAndInterrupt() throws InterruptedException {
        // given
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        OutboxAutoConfiguration spyTested = spy(tested);
        doReturn(executor).when(spyTested).outboxScheduledExecutorService();
        when(executor.awaitTermination(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException());

        // when
        spyTested.onDestroy();

        // then
        verify(executor).shutdown();
        verify(executor).shutdownNow();
    }
}
