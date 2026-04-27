package io.github.dmitriyiliyov.springoutbox.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FixedOutboxScheduleStrategy implements OutboxScheduleStrategy {

    private static final Logger log = LoggerFactory.getLogger(FixedOutboxScheduleStrategy.class);

    private final FixedPollingPropertiesHolder properties;
    private final ScheduledExecutorService executor;
    private final OutboxScheduleStrategyListener listener;

    public FixedOutboxScheduleStrategy(FixedPollingPropertiesHolder properties,
                                       ScheduledExecutorService executor,
                                       OutboxScheduleStrategyListener listener) {
        this.properties = properties;
        this.executor = executor;
        this.listener = listener;
    }

    @Override
    public void scheduleExecution(ContinuableTask task) {
        listener.onDelayChanged(properties.getFixedDelay().toMillis());
        executor.scheduleWithFixedDelay(
                () -> {
                    listener.onExecutionStarted();
                    try {
                        task.run();
                        listener.onExecutionSucceeded();
                    } catch (Throwable t) {
                        listener.onExecutionFailed();
                        log.error("Exception in scheduled execution", t);
                    }
                },
                properties.getInitialDelay().toMillis(),
                properties.getFixedDelay().toMillis(),
                TimeUnit.MILLISECONDS
        );
    }
}
