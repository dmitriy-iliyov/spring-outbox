package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

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

public final class OutboxDlqCleanUpScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxDlqCleanUpScheduler.class);
    private static final OutboxJob JOB = OutboxJob.OUTBOX_DLQ_CLEANUP;

    private final UUID workerId;
    private final OutboxPropertiesHolder.CleanUpPropertiesHolder properties;
    private final OutboxScheduleStrategy strategy;
    private final ContinuableTaskDecorator continuableTaskDecorator;
    private final OutboxDlqManager manager;
    private final DistributedLockRepository lock;

    public OutboxDlqCleanUpScheduler(UUID workerId,
                                     OutboxPropertiesHolder.CleanUpPropertiesHolder properties,
                                     OutboxScheduleStrategy strategy,
                                     ContinuableTaskDecorator continuableTaskDecorator,
                                     OutboxDlqManager manager,
                                     DistributedLockRepository lock) {
        this.workerId = workerId;
        this.properties = properties;
        this.strategy = strategy;
        this.continuableTaskDecorator = continuableTaskDecorator;
        this.manager = manager;
        this.lock = lock;
    }

    @Override
    public void schedule() {
        ContinuableTask task = () -> {
            log.debug("Start clean up resolved DLQ events");
            if (!lock.tryLock(JOB.getJobName(), workerId)) {
                log.debug("Lock acquired by another instance; skipping task execution");
                return false;
            }
            try {
                int batchSize = properties.getBatchSize();
                int deletedCount = manager.deleteResolvedBatch(properties.getTtl(), batchSize);
                log.debug("Successfully cleaned {} events", deletedCount);
                return batchSize == deletedCount;
            } catch (Exception e) {
                log.error("Error process clean up resolved DLQ events", e);
                return false;
            } finally {
                lock.unlock(JOB.getJobName(), workerId);
            }
        };
        strategy.scheduleExecution(continuableTaskDecorator.decorate(task));
    }
}
