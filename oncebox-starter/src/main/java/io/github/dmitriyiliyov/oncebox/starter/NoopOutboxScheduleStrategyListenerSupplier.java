package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.polling.OutboxScheduleStrategyListener;

public class NoopOutboxScheduleStrategyListenerSupplier implements OutboxScheduleStrategyListenerSupplier {
    @Override
    public OutboxScheduleStrategyListener supply(String taskType) {
        return OutboxScheduleStrategyListener.NOOP;
    }
}
