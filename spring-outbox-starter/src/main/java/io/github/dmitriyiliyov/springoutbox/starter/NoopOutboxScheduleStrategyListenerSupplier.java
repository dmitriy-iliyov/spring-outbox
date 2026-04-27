package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduleStrategyListener;

public final class NoopOutboxScheduleStrategyListenerSupplier implements OutboxScheduleStrategyListenerSupplier {
    @Override
    public OutboxScheduleStrategyListener supply(String taskType) {
        return OutboxScheduleStrategyListener.NOOP;
    }
}
