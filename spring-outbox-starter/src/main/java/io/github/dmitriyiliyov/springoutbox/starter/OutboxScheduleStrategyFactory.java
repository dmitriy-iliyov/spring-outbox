package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.AdaptiveOutboxScheduleStrategy;
import io.github.dmitriyiliyov.springoutbox.core.FixedOutboxScheduleStrategy;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduleStrategy;

import java.util.concurrent.ScheduledExecutorService;

/**
 * A factory for creating {@link OutboxScheduleStrategy} instances based on the pollingDefaults configuration.
 * <p>
 * It supports both {@link PollingType#FIXED} and {@link PollingType#ADAPTIVE} pollingDefaults strategies.
 */
public final class OutboxScheduleStrategyFactory {

    private OutboxScheduleStrategyFactory() {}

    /**
     * Generates an {@link OutboxScheduleStrategy} instance based on the provided pollingDefaults properties.
     *
     * @param properties the pollingDefaults properties determining the strategy type ({@link PollingType#FIXED} or {@link PollingType#ADAPTIVE}).
     * @param executor   the scheduled executor service for task scheduling.
     * @return           a configured {@link OutboxScheduleStrategy} instance.
     * @throws IllegalStateException if an unknown or unsupported pollingDefaults type is encountered.
     */
    public static OutboxScheduleStrategy create(OutboxProperties.PollingProperties properties,
                                                ScheduledExecutorService executor) {
        if (PollingType.FIXED.equals(properties.getType())) {
            return new FixedOutboxScheduleStrategy(properties, executor);
        } else if (PollingType.ADAPTIVE.equals(properties.getType())) {
            return new AdaptiveOutboxScheduleStrategy(properties, executor);
        } else {
            throw new IllegalStateException("Reached unreachable branch during creating OutboxScheduleStrategy");
        }
    }
}
