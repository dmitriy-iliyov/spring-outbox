package io.github.dmitriyiliyov.springoutbox.core.publisher;


import io.github.dmitriyiliyov.springoutbox.core.ContinuableTask;
import io.github.dmitriyiliyov.springoutbox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.core.polling.OutboxScheduleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OutboxRecoveryScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRecoveryScheduler.class);

    private final OutboxPublisherPropertiesHolder.StuckRecoveryPropertiesHolder properties;
    private final OutboxScheduleStrategy strategy;
    private final OutboxManager manager;
    private final ContinuableTaskDecorator continuableTaskDecorator;

    public OutboxRecoveryScheduler(OutboxPublisherPropertiesHolder.StuckRecoveryPropertiesHolder properties,
                                   OutboxScheduleStrategy strategy,
                                   OutboxManager manager,
                                   ContinuableTaskDecorator continuableTaskDecorator) {
        this.properties = properties;
        this.strategy = strategy;
        this.manager = manager;
        this.continuableTaskDecorator = continuableTaskDecorator;
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
        strategy.scheduleExecution(continuableTaskDecorator.decorate(continuableTask));
    }
}
