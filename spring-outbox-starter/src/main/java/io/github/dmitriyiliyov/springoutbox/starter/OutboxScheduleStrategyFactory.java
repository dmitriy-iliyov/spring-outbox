package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.polling.AdaptiveOutboxScheduleStrategy;
import io.github.dmitriyiliyov.springoutbox.core.polling.FixedOutboxScheduleStrategy;
import io.github.dmitriyiliyov.springoutbox.core.polling.OutboxScheduleStrategy;
import io.github.dmitriyiliyov.springoutbox.core.polling.OutboxScheduleStrategyListener;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A factory for creating {@link OutboxScheduleStrategy} instances based on the polling configuration.
 * <p>
 * Implementations should support scheduling strategies based on {@link PollingType}, such as
 * {@link PollingType#FIXED} and {@link PollingType#ADAPTIVE}.
 */
public final class OutboxScheduleStrategyFactory {

    private OutboxScheduleStrategyFactory() {}

    /**
     * Creates an {@link OutboxScheduleStrategy} instance based on the provided task type and polling properties.
     *
     * @param taskType         an identifier representing the type of task to be scheduled.
     * @param properties       the polling properties determining the strategy type ({@link PollingType#FIXED} or {@link PollingType#ADAPTIVE}).
     * @param executor         the scheduled executor service used for task scheduling.
     * @param listenerSupplier a supplier for providing a {@link OutboxScheduleStrategyListener} based on taskType.
     * @return                 a configured {@link OutboxScheduleStrategy} instance.
     * @throws NullPointerException  if some of the passed parameters is null.
     * @throws IllegalStateException if an unknown or unsupported polling type is encountered.
     */
    public static OutboxScheduleStrategy create(String taskType,
                                                OutboxProperties.PollingProperties properties,
                                                ScheduledExecutorService executor,
                                                OutboxScheduleStrategyListenerSupplier listenerSupplier) {
        Objects.requireNonNull(taskType, "taskType cannot be null");
        Objects.requireNonNull(properties, "properties cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(listenerSupplier, "listenerSupplier cannot be null");

        if (PollingType.FIXED.equals(properties.getType())) {
            return new FixedOutboxScheduleStrategy(properties, executor, listenerSupplier.supply(taskType));
        } else if (PollingType.ADAPTIVE.equals(properties.getType())) {
            return new AdaptiveOutboxScheduleStrategy(properties, executor, listenerSupplier.supply(taskType));
        } else {
            throw new IllegalStateException("Reached unreachable branch during creating OutboxScheduleStrategy");
        }
    }
}
