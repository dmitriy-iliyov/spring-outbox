package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.polling.OutboxScheduleStrategyListener;

/**
 * A supplier for creating {@link OutboxScheduleStrategyListener} instances.
 */
public interface OutboxScheduleStrategyListenerSupplier {
    
    /**
     * Supplies a configured {@link OutboxScheduleStrategyListener} for the given task type.
     *
     * @param taskType the type of task for which to provide the listener.
     * @return a configured {@link OutboxScheduleStrategyListener}.
     */
    OutboxScheduleStrategyListener supply(String taskType);
}
