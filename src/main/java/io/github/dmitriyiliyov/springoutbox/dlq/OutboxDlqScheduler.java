package io.github.dmitriyiliyov.springoutbox.dlq;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class OutboxDlqScheduler implements OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxDlqScheduler.class);

    private final OutboxProperties.DlqProperties properties;
    private final ScheduledExecutorService executor;
    private final OutboxDlqTransfer transfer;

    public OutboxDlqScheduler(OutboxProperties.DlqProperties properties, ScheduledExecutorService executor,
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
                        transfer.transferOutboxToDlq(properties.batchSize());
                    } catch (Exception e) {
                        log.error("Error process transfer failed events from outbox to DLQ", e);
                    }
                },
                properties.transferToDlqInitialDelay().toSeconds(),
                properties.transferToDlqFixedDelay().toSeconds(),
                TimeUnit.SECONDS
        );
        executor.scheduleWithFixedDelay(
                () -> {
                    try {
                        transfer.transferDlqToOutbox(properties.batchSize());
                    } catch (Exception e) {
                        log.error("Error process transfer failed events from DLQ to outbox to retry", e);
                    }
                },
                properties.transferFromDlqInitialDelay().toSeconds(),
                properties.transferFormDlqFixedDelay().toSeconds(),
                TimeUnit.SECONDS
        );
    }
}
