package io.github.dmitriyiliyov.springoutbox.core.publisher;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduleStrategy;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OutboxPollingScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPollingScheduler.class);

    private final OutboxPublisherPropertiesHolder.EventPropertiesHolder properties;
    private final OutboxProcessor processor;
    private final OutboxScheduleStrategy strategy;

    public OutboxPollingScheduler(OutboxPublisherPropertiesHolder.EventPropertiesHolder properties,
                                  OutboxScheduleStrategy strategy,
                                  OutboxProcessor processor) {
        this.properties = properties;
        this.processor = processor;
        this.strategy = strategy;
    }

    @Override
    public void schedule() {
        strategy.scheduleExecution(() -> {
            int batchSize = properties.getBatchSize();
            int processedCount = 0;
            try {
                log.debug("Start processing {} outbox events", properties.getEventType());
                processedCount = processor.process(properties);
            } catch (Exception e) {
                log.error("Error process outbox events for type={}", properties.getEventType(), e);
            }
            return processedCount == batchSize;
        });
    }
}
