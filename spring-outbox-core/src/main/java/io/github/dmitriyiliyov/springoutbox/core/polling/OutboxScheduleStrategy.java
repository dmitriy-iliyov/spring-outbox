package io.github.dmitriyiliyov.springoutbox.core.polling;

import io.github.dmitriyiliyov.springoutbox.core.ContinuableTask;

public interface OutboxScheduleStrategy {

    /**
     * Schedules the given task for execution.
     *
     * @param task the task to be scheduled. Its execution continues as long as it returns {@code true}.
     */
    void scheduleExecution(ContinuableTask task);
}
