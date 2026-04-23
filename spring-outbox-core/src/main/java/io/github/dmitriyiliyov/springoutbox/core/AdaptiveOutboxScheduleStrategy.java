package io.github.dmitriyiliyov.springoutbox.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class AdaptiveOutboxScheduleStrategy implements OutboxScheduleStrategy {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveOutboxScheduleStrategy.class);

    private final AdaptivePollingPropertiesHolder properties;
    private final ScheduledExecutorService executor;
    private final long minFixedDelay;
    private final long maxFixedDelay;
    private final double multiplier;
    private final AtomicLong currentDelay;
    private final AtomicBoolean taskInProcess;

    public AdaptiveOutboxScheduleStrategy(AdaptivePollingPropertiesHolder properties,
                                          ScheduledExecutorService executor) {
        this.properties = properties;
        this.executor = executor;
        this.minFixedDelay = properties.getMinFixedDelay().toMillis();
        this.maxFixedDelay = properties.getMaxFixedDelay().toMillis();
        this.multiplier = properties.getMultiplier();
        this.currentDelay = new AtomicLong(minFixedDelay);
        this.taskInProcess = new AtomicBoolean(false);
    }

    @Override
    public void scheduleExecution(Continuable task) {
        scheduleNext(task, properties.getInitialDelay().toMillis());
    }

    private void scheduleNext(Continuable task, long delay) {
        if (executor.isShutdown() || !taskInProcess.compareAndSet(false, true)) {
            return;
        }
        try {
            executor.schedule(
                    () -> {
                        try {
                            executeTask(task);
                        } catch (Throwable t) {
                            log.error("Exception in scheduled execution", t);
                        }
                    },
                    delay,
                    TimeUnit.MILLISECONDS
            );
        } catch (RejectedExecutionException e) {
            taskInProcess.set(false);
            log.warn("Executor rejected scheduling, likely shutting down", e);
        }
    }

    private void executeTask(Continuable task) {
        boolean shouldContinue = false;
        try {
            shouldContinue = task.run();
        } catch (Throwable t) {
            log.error("Exception while executing task", t);
        } finally {
            if (shouldContinue) {
                currentDelay.set(minFixedDelay);
            } else {
                currentDelay.updateAndGet(d -> Math.min((long) (multiplier * d), maxFixedDelay));
                log.debug("Schedule strategy adapted, current delay is %dms".formatted(currentDelay.get()));
            }
            taskInProcess.set(false);
            if (!executor.isShutdown()) {
                scheduleNext(task, currentDelay.get());
            }
        }
    }
}