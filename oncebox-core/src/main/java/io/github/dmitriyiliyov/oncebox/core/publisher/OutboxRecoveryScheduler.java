package io.github.dmitriyiliyov.oncebox.core.publisher;


import io.github.dmitriyiliyov.oncebox.core.ContinuableTask;
import io.github.dmitriyiliyov.oncebox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.oncebox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.oncebox.core.OutboxScheduler;
import io.github.dmitriyiliyov.oncebox.core.polling.OutboxScheduleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public final class OutboxRecoveryScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRecoveryScheduler.class);

    private final OutboxPublisherPropertiesHolder.StuckRecoveryPropertiesHolder properties;
    private final OutboxScheduleStrategy scheduleStrategy;
    private final OutboxManager manager;
    private final ContinuableTaskDecorator taskDecorator;

    public OutboxRecoveryScheduler(OutboxPublisherPropertiesHolder.StuckRecoveryPropertiesHolder properties,
                                   OutboxScheduleStrategy scheduleStrategy,
                                   OutboxManager manager,
                                   ContinuableTaskDecorator taskDecorator) {
        this.properties = Objects.requireNonNull(properties, "properties cannot be null");
        this.scheduleStrategy = Objects.requireNonNull(scheduleStrategy, "scheduleStrategy cannot be null");
        this.manager = Objects.requireNonNull(manager, "manager cannot be null");
        this.taskDecorator = Objects.requireNonNull(taskDecorator, "taskDecorator cannot be null");
    }

    @Override
    public void schedule() {
        ContinuableTask continuableTask = () -> {
            int batchSize = properties.getBatchSize();
            int recoveredCount = 0;
            try {
                log.debug("Start recovering stuck outbox events");
                recoveredCount = manager.recoverStuckBatch(properties.getMaxBatchProcessingTime(), batchSize);
            } catch (Exception e) {
                log.error("Error process recover stuck outbox events", e);
            }
            return recoveredCount == batchSize;
        };
        scheduleStrategy.scheduleExecution(taskDecorator.decorate(continuableTask));
    }
}
