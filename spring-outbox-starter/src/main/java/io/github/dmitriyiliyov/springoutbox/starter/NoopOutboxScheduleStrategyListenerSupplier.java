package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.polling.OutboxScheduleStrategyListener;

public class NoopOutboxScheduleStrategyListenerSupplier implements OutboxScheduleStrategyListenerSupplier {
    @Override
    public OutboxScheduleStrategyListener supply(String taskType) {
        return OutboxScheduleStrategyListener.NOOP;
    }
}
