package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduleStrategy;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OutboxDlqTransferScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxDlqTransferScheduler.class);

    private final OutboxPublisherPropertiesHolder.DlqPropertiesHolder properties;
    private final OutboxScheduleStrategy transferToStrategy;
    private final OutboxScheduleStrategy transferFromStrategy;
    private final OutboxDlqTransfer transfer;

    public OutboxDlqTransferScheduler(OutboxPublisherPropertiesHolder.DlqPropertiesHolder properties,
                                      OutboxScheduleStrategy transferToStrategy,
                                      OutboxScheduleStrategy transferFromStrategy,
                                      OutboxDlqTransfer transfer) {
        this.properties = properties;
        this.transferToStrategy = transferToStrategy;
        this.transferFromStrategy = transferFromStrategy;
        this.transfer = transfer;
    }

    @Override
    public void schedule() {
        transferToStrategy.scheduleExecution(() -> {
            int batchSize = properties.getTransferTo().getBatchSize();
            int transferredCount = 0;
            try {
                log.debug("Start transferring failed outbox events to DLQ");
                transferredCount = transfer.transferToDlq(batchSize);
            } catch (Exception e) {
                log.error("Error process transfer failed events from outbox to DLQ", e);
            }
            return transferredCount == batchSize;
        });

        transferFromStrategy.scheduleExecution(() -> {
            int batchSize = properties.getTransferFrom().getBatchSize();
            int transferredCount = 0;
            try {
                log.debug("Start transferring events from DLQ to outbox");
                transferredCount = transfer.transferFromDlq(batchSize);
            } catch (Exception e) {
                log.error("Error process transfer failed events from DLQ to outbox to retry", e);
            }
            return transferredCount == batchSize;
        });
    }
}
