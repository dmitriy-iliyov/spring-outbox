package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.ContinuableTask;
import io.github.dmitriyiliyov.springoutbox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.core.polling.OutboxScheduleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;
import java.util.function.Supplier;

public final class OutboxDlqTransferScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxDlqTransferScheduler.class);

    private final OutboxPublisherPropertiesHolder.DlqPropertiesHolder.TransferPropertiesHolder properties;
    private final OutboxScheduleStrategy transferScheduleStrategy;
    private final Function<Integer, Integer> transferApplier;
    private final ContinuableTaskDecorator transferContinuableTaskDecorator;
    private final LogMessage logMessage;

    public OutboxDlqTransferScheduler(Supplier<OutboxPublisherPropertiesHolder.DlqPropertiesHolder.TransferPropertiesHolder> propertiesSupplier,
                                      OutboxScheduleStrategy strategy,
                                      Function<Integer, Integer> transferApplier,
                                      ContinuableTaskDecorator continuableTaskDecorator,
                                      LogMessage logMessage) {
        this.properties = propertiesSupplier.get();
        this.transferScheduleStrategy = strategy;
        this.transferApplier = transferApplier;
        this.transferContinuableTaskDecorator = continuableTaskDecorator;
        this.logMessage = logMessage;
    }

    @Override
    public void schedule() {
        ContinuableTask task = () -> {
            int batchSize = properties.getBatchSize();
            int transferredCount = 0;
            try {
                log.debug(logMessage.onStart());
                transferredCount = transferApplier.apply(batchSize);
            } catch (Exception e) {
                log.error(logMessage.onException(), e);
            }
            return transferredCount == batchSize;
        };
        transferScheduleStrategy.scheduleExecution(transferContinuableTaskDecorator.decorate(task));
    }

    public record LogMessage(
            String onStart,
            String onException
    ) {
        public static LogMessage transferTo() {
            return new LogMessage(
                    "Start transferring outbox events to DLQ",
                    "Error transferring outbox events to DLQ"
            );
        }

        public static LogMessage transferFrom() {
            return new LogMessage(
                    "Start transferring outbox events from DLQ",
                    "Error transferring outbox events from DLQ"
            );
        }
    }
}
