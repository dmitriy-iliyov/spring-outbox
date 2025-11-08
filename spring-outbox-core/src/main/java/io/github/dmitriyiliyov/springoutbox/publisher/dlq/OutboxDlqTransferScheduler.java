package io.github.dmitriyiliyov.springoutbox.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherProperties;
import io.github.dmitriyiliyov.springoutbox.publisher.core.OutboxScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class OutboxDlqTransferScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxDlqTransferScheduler.class);

    private final OutboxPublisherProperties.DlqProperties properties;
    private final ScheduledExecutorService executor;
    private final OutboxDlqTransfer transfer;

    public OutboxDlqTransferScheduler(OutboxPublisherProperties.DlqProperties properties, ScheduledExecutorService executor,
                                      OutboxDlqTransfer transfer) {
        this.properties = properties;
        this.executor = executor;
        this.transfer = transfer;
    }

    @Override
    public void schedule() {
        executor.scheduleWithFixedDelay(
                () -> {
                    try {
                        log.debug("Start transferring failed outbox events to DLQ");
                        transfer.transferToDlq(properties.getBatchSize());
                    } catch (Exception e) {
                        log.error("Error process transfer failed events from outbox to DLQ", e);
                    }
                },
                properties.getTransferToInitialDelay().toSeconds(),
                properties.getTransferToFixedDelay().toSeconds(),
                TimeUnit.SECONDS
        );
        executor.scheduleWithFixedDelay(
                () -> {
                    try {
                        log.debug("Start transferring events from DLQ to outbox");
                        transfer.transferFromDlq(properties.getBatchSize());
                    } catch (Exception e) {
                        log.error("Error process transfer failed events from DLQ to outbox to retry", e);
                    }
                },
                properties.getTransferFromInitialDelay().toSeconds(),
                properties.getTransferFromFixedDelay().toSeconds(),
                TimeUnit.SECONDS
        );
    }
}
