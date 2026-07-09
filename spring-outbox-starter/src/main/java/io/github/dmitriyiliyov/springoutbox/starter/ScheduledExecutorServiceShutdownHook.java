package io.github.dmitriyiliyov.springoutbox.starter;

/**
 * Shutdown hook for the outbox ScheduledExecutorService.
 */
public interface ScheduledExecutorServiceShutdownHook {
    void shutdown();
}
