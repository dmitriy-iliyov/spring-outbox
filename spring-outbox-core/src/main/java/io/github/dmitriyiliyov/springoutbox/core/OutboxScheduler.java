package io.github.dmitriyiliyov.springoutbox.core;

/**
 * Abstraction for scheduling outbox tasks.
 */
public interface OutboxScheduler {
    void schedule();
}
