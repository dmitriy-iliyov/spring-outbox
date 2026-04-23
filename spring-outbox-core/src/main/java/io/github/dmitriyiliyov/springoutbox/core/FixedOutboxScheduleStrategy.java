package io.github.dmitriyiliyov.springoutbox.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FixedOutboxScheduleStrategy implements OutboxScheduleStrategy {

    private static final Logger log = LoggerFactory.getLogger(FixedOutboxScheduleStrategy.class);

    private final FixedPollingPropertiesHolder properties;
    private final ScheduledExecutorService executor;

    public FixedOutboxScheduleStrategy(FixedPollingPropertiesHolder properties,
                                       ScheduledExecutorService executor) {
        this.properties = properties;
        this.executor = executor;
    }

    @Override
    public void scheduleExecution(Continuable task) {
        executor.scheduleWithFixedDelay(
                () -> {
                    try {
                        task.run();
                    } catch (Throwable t) {
                        log.error("Exception in scheduled execution", t);
                    }
                },
                properties.getInitialDelay().toMillis(),
                properties.getFixedDelay().toMillis(),
                TimeUnit.MILLISECONDS
        );
    }
}
