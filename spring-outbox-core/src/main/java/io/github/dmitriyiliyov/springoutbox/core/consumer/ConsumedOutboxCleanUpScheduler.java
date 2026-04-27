package io.github.dmitriyiliyov.springoutbox.core.consumer;

import io.github.dmitriyiliyov.springoutbox.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConsumedOutboxCleanUpScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(ConsumedOutboxCleanUpScheduler.class);

    private final OutboxPropertiesHolder.CleanUpPropertiesHolder properties;
    private final OutboxScheduleStrategy strategy;
    private final ConsumedOutboxManager manager;
    private final ContinuableTaskDecorator continuableTaskDecorator;

    public ConsumedOutboxCleanUpScheduler(OutboxPropertiesHolder.CleanUpPropertiesHolder properties,
                                          OutboxScheduleStrategy strategy,
                                          ConsumedOutboxManager manager,
                                          ContinuableTaskDecorator continuableTaskDecorator) {
        this.properties = properties;
        this.strategy = strategy;
        this.manager = manager;
        this.continuableTaskDecorator = continuableTaskDecorator;
    }

    @Override
    public void schedule() {
        ContinuableTask task = () -> {
            int cleanedCount = 0;
            try {
                log.debug("Start clean up consumed events");
                cleanedCount = manager.cleanBatchByTtl(properties.getTtl(), properties.getBatchSize());
            } catch (Exception e) {
                log.error("Error when cleanup consumed outbox events", e);
            }
            return cleanedCount == properties.getBatchSize();
        };
        strategy.scheduleExecution(continuableTaskDecorator.decorate(task));
    }
}
