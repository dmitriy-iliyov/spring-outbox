package io.github.dmitriyiliyov.springoutbox.core.publisher;

import io.github.dmitriyiliyov.springoutbox.core.ContinuableTask;
import io.github.dmitriyiliyov.springoutbox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.core.polling.OutboxScheduleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public final class OutboxPollingScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPollingScheduler.class);

    private final OutboxPublisherPropertiesHolder.EventPropertiesHolder properties;
    private final OutboxProcessor processor;
    private final OutboxScheduleStrategy scheduleStrategy;
    private final ContinuableTaskDecorator taskDecorator;

    public OutboxPollingScheduler(OutboxPublisherPropertiesHolder.EventPropertiesHolder properties,
                                  OutboxScheduleStrategy scheduleStrategy,
                                  OutboxProcessor processor,
                                  ContinuableTaskDecorator taskDecorator) {
        this.properties = Objects.requireNonNull(properties, "properties cannot be null");
        this.scheduleStrategy = Objects.requireNonNull(scheduleStrategy, "scheduleStrategy cannot be null");
        this.processor = Objects.requireNonNull(processor, "processor cannot be null");
        this.taskDecorator = Objects.requireNonNull(taskDecorator, "taskDecorator cannot be null");
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
        scheduleStrategy.scheduleExecution(taskDecorator.decorate(task));
    }
}
