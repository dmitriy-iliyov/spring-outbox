package io.github.dmitriyiliyov.springoutbox.dlq;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class OutboxDlqScheduler implements OutboxScheduler {

    private final OutboxProperties.DlqProperties properties;
    private final DlqTransfer transfer;
    private final ScheduledExecutorService executor;

    public OutboxDlqScheduler(OutboxProperties.DlqProperties properties, DlqTransfer transfer,
                              ScheduledExecutorService executor) {
        this.properties = properties;
        this.transfer = transfer;
        this.executor = executor;
    }

    @Override
    public void schedule() {
        executor.scheduleWithFixedDelay(
                () -> transfer.transferOutboxToDlq(properties.batchSize()),
                properties.initialDelay().getSeconds(),
                properties.fixedDelay().getSeconds(),
                TimeUnit.SECONDS
        );
        executor.scheduleWithFixedDelay(
                () -> transfer.transferDlqToOutbox(properties.batchSize()),
                properties.initialDelay().getSeconds(),
                properties.fixedDelay().getSeconds(),
                TimeUnit.SECONDS
        );
    }
}
