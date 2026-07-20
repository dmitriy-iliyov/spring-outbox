package io.github.dmitriyiliyov.oncebox.core;

/**
 * Abstraction for scheduling outbox tasks.
 */
public interface OutboxScheduler {
    void schedule();
}
