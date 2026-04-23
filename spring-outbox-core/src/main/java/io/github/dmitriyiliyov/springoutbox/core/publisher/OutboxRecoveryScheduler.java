package io.github.dmitriyiliyov.springoutbox.core.publisher;


import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduleStrategy;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OutboxRecoveryScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRecoveryScheduler.class);

    private final OutboxPublisherPropertiesHolder.StuckRecoveryPropertiesHolder properties;
    private final OutboxScheduleStrategy strategy;
    private final OutboxManager manager;

    public OutboxRecoveryScheduler(OutboxPublisherPropertiesHolder.StuckRecoveryPropertiesHolder properties,
                                   OutboxScheduleStrategy strategy,
                                   OutboxManager manager) {
        this.properties = properties;
        this.strategy = strategy;
        this.manager = manager;
    }

    @Override
    public void schedule() {
        strategy.scheduleExecution(() -> {
            int batchSize = properties.getBatchSize();
            int recoveredCount = 0;
            try {
                log.debug("Start recovering stuck outbox events");
                recoveredCount = manager.recoverStuckBatch(properties.getMaxBatchProcessingTime(), batchSize);
            } catch (Exception e) {
                log.error("Error process recover stuck outbox events", e);
            }
            return recoveredCount == batchSize;
        });
    }
}
