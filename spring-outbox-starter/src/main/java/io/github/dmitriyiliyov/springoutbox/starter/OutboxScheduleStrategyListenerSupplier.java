package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduleStrategyListener;

public interface OutboxScheduleStrategyListenerSupplier {
    OutboxScheduleStrategyListener supply(String taskType);
}
