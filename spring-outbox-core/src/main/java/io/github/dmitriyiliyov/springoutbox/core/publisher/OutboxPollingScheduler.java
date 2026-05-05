package io.github.dmitriyiliyov.springoutbox.core.publisher;

import io.github.dmitriyiliyov.springoutbox.core.ContinuableTask;
import io.github.dmitriyiliyov.springoutbox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.core.polling.OutboxScheduleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OutboxPollingScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPollingScheduler.class);

    private final OutboxPublisherPropertiesHolder.EventPropertiesHolder properties;
    private final OutboxProcessor processor;
    private final OutboxScheduleStrategy strategy;
    private final ContinuableTaskDecorator continuableTaskDecorator;

    public OutboxPollingScheduler(OutboxPublisherPropertiesHolder.EventPropertiesHolder properties,
                                  OutboxScheduleStrategy strategy,
                                  OutboxProcessor processor,
                                  ContinuableTaskDecorator continuableTaskDecorator) {
        this.properties = properties;
        this.strategy = strategy;
        this.processor = processor;
        this.continuableTaskDecorator = continuableTaskDecorator;
    }

    @Override
    public void schedule() {
        ContinuableTask task = () -> {
            int batchSize = properties.getBatchSize();
            int processedCount = 0;
            try {
                log.debug("Start processing {} outbox events", properties.getEventType());
                processedCount = processor.process(properties);
            } catch (Exception e) {
                log.error("Error process outbox events for type={}", properties.getEventType(), e);
            }
            return processedCount == batchSize;
        };
        strategy.scheduleExecution(continuableTaskDecorator.decorate(task));
    }
}
