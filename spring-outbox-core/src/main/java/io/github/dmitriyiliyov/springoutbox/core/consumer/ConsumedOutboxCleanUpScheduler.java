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

import java.util.Objects;
import java.util.UUID;

public final class ConsumedOutboxCleanUpScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(ConsumedOutboxCleanUpScheduler.class);
    private static final OutboxJob JOB = OutboxJob.OUTBOX_CONSUMED_CLEANUP;

    private final UUID workerId;
    private final OutboxPropertiesHolder.CleanUpPropertiesHolder properties;
    private final OutboxScheduleStrategy scheduleStrategy;
    private final ConsumedOutboxManager manager;
    private final DistributedLockRepository lock;
    private final ContinuableTaskDecorator taskDecorator;

    public ConsumedOutboxCleanUpScheduler(UUID workerId,
                                          OutboxPropertiesHolder.CleanUpPropertiesHolder properties,
                                          OutboxScheduleStrategy scheduleStrategy,
                                          ConsumedOutboxManager manager,
                                          DistributedLockRepository lock,
                                          ContinuableTaskDecorator taskDecorator) {
        this.workerId = Objects.requireNonNull(workerId, "workerId cannot be null");
        this.properties = Objects.requireNonNull(properties, "properties cannot be null");
        this.scheduleStrategy = Objects.requireNonNull(scheduleStrategy, "scheduleStrategy cannot be null");
        this.manager = Objects.requireNonNull(manager, "manager cannot be null");
        this.lock = Objects.requireNonNull(lock, "lock cannot be null");
        this.taskDecorator = Objects.requireNonNull(taskDecorator, "taskDecorator cannot be null");
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
        scheduleStrategy.scheduleExecution(taskDecorator.decorate(task));
    }
}
