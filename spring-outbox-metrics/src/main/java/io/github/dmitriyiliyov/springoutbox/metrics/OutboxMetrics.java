package io.github.dmitriyiliyov.springoutbox.metrics;

/**
 * Abstraction for registering outbox-related metrics.
 * Implementations of this interface are responsible for setting up and exposing
 * metrics to a monitoring system.
 */
public interface OutboxMetrics {

    /**
     * Registers all relevant outbox metrics.
     */
    void register();
}
