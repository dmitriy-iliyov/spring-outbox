package io.github.dmitriyiliyov.springoutbox.dlq;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class OutboxDlqScheduler implements OutboxScheduler {

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
                () -> transfer.transferOutboxToDlq(properties.batchSize()),
                properties.transferToDlqInitialDelay().getSeconds(),
                properties.transferToDlqFixedDelay().getSeconds(),
                TimeUnit.SECONDS
        );
        executor.scheduleWithFixedDelay(
                () -> transfer.transferDlqToOutbox(properties.batchSize()),
                properties.transferFromDlqInitialDelay().getSeconds(),
                properties.transferFormDlqFixedDelay().getSeconds(),
                TimeUnit.SECONDS
        );
    }
}
