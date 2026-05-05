package io.github.dmitriyiliyov.springoutbox.core.consumer;

import io.github.dmitriyiliyov.springoutbox.core.ContinuableTask;
import io.github.dmitriyiliyov.springoutbox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.springoutbox.core.OutboxPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.springoutbox.core.locks.OutboxJob;
import io.github.dmitriyiliyov.springoutbox.core.polling.OutboxScheduleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public final class ConsumedOutboxCleanUpScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(ConsumedOutboxCleanUpScheduler.class);
    private static final OutboxJob JOB = OutboxJob.OUTBOX_CONSUMED_CLEANUP;

    private final UUID workerId;
    private final OutboxPropertiesHolder.CleanUpPropertiesHolder properties;
    private final OutboxScheduleStrategy strategy;
    private final ConsumedOutboxManager manager;
    private final DistributedLockRepository lock;
    private final ContinuableTaskDecorator continuableTaskDecorator;

    public ConsumedOutboxCleanUpScheduler(UUID workerId,
                                          OutboxPropertiesHolder.CleanUpPropertiesHolder properties,
                                          OutboxScheduleStrategy strategy,
                                          ConsumedOutboxManager manager,
                                          DistributedLockRepository lock,
                                          ContinuableTaskDecorator continuableTaskDecorator) {
        this.workerId = workerId;
        this.properties = properties;
        this.strategy = strategy;
        this.manager = manager;
        this.lock = lock;
        this.continuableTaskDecorator = continuableTaskDecorator;
    }

    @Override
    public void schedule() {
        ContinuableTask task = () -> {
            log.debug("Start clean up consumed events");
            if (!lock.tryLock(JOB.getJobName(), workerId)) {
                log.debug("Lock acquired by another instance; skipping task execution");
                return false;
            }
            try {
                int batchSize = properties.getBatchSize();
                int cleanedCount = manager.cleanBatchByTtl(properties.getTtl(), batchSize);
                log.debug("Successfully cleaned {} events", cleanedCount);
                return cleanedCount == batchSize;
            } catch (Exception e) {
                log.error("Error when cleanup consumed outbox events", e);
                return false;
            } finally {
                lock.unlock(JOB.getJobName(), workerId);
            }
        };
        strategy.scheduleExecution(continuableTaskDecorator.decorate(task));
    }
}
