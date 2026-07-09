package io.github.dmitriyiliyov.springoutbox.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultScheduledExecutorServiceShutdownHookIntegrationTests {

    @Test
    @DisplayName("IT should gracefully terminate idle real ScheduledExecutorService")
    void shutdown_whenExecutorIsIdle_shouldTerminateGracefully() {
        // given
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        DefaultScheduledExecutorServiceShutdownHook hook =
                new DefaultScheduledExecutorServiceShutdownHook(executorService);

        // when
        hook.shutdown();

        // then
        assertTrue(executorService.isShutdown());
        assertTrue(executorService.isTerminated());
    }

    @Test
    @DisplayName("IT should terminate real ScheduledExecutorService after a pending task completes")
    void shutdown_whenExecutorHasQuicklyCompletingTask_shouldTerminateAfterTaskCompletion() {
        // given
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        AtomicBoolean taskExecuted = new AtomicBoolean(false);

        executorService.schedule(() -> taskExecuted.set(true), 50, TimeUnit.MILLISECONDS);

        DefaultScheduledExecutorServiceShutdownHook hook =
                new DefaultScheduledExecutorServiceShutdownHook(executorService);

        // when
        hook.shutdown();

        // then
        assertTrue(executorService.isShutdown());
        assertTrue(executorService.isTerminated());
        assertTrue(taskExecuted.get(), "Pending task should have been executed before shutdown completed");
    }

    @Test
    @DisplayName("IT should shut down real ScheduledExecutorService that has a blocking task via forced shutdown")
    void shutdown_whenExecutorHasBlockingTask_shouldForceShutdown() throws InterruptedException {
        // given
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        executorService.submit(() -> {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.sleep(50);

        DefaultScheduledExecutorServiceShutdownHook hook =
                new DefaultScheduledExecutorServiceShutdownHook(executorService);

        // when
        hook.shutdown();

        // then
        assertTrue(executorService.isShutdown());
    }
}