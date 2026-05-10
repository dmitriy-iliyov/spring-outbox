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

import java.util.Objects;
import java.util.UUID;

public final class OutboxDlqCleanUpScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxDlqCleanUpScheduler.class);
    private static final OutboxJob JOB = OutboxJob.OUTBOX_DLQ_CLEANUP;

    private final UUID workerId;
    private final OutboxPropertiesHolder.CleanUpPropertiesHolder properties;
    private final OutboxScheduleStrategy scheduleStrategy;
    private final ContinuableTaskDecorator taskDecorator;
    private final OutboxDlqManager manager;
    private final DistributedLockRepository lock;

    public OutboxDlqCleanUpScheduler(UUID workerId,
                                     OutboxPropertiesHolder.CleanUpPropertiesHolder properties,
                                     OutboxScheduleStrategy scheduleStrategy,
                                     ContinuableTaskDecorator taskDecorator,
                                     OutboxDlqManager manager,
                                     DistributedLockRepository lock) {
        this.workerId = Objects.requireNonNull(workerId, "workerId cannot be null");
        this.properties = Objects.requireNonNull(properties, "properties cannot be null");
        this.scheduleStrategy = Objects.requireNonNull(scheduleStrategy, "scheduleStrategy cannot be null");
        this.taskDecorator = Objects.requireNonNull(taskDecorator, "taskDecorator cannot be null");
        this.manager = Objects.requireNonNull(manager, "manager cannot be null");
        this.lock = Objects.requireNonNull(lock, "lock cannot be null");
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
        scheduleStrategy.scheduleExecution(taskDecorator.decorate(task));
    }
}
