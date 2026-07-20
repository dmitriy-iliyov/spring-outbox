package io.github.dmitriyiliyov.oncebox.starter;

/**
 * Shutdown hook for the outbox ScheduledExecutorService.
 */
public interface ScheduledExecutorServiceShutdownHook {
    void shutdown();
}
