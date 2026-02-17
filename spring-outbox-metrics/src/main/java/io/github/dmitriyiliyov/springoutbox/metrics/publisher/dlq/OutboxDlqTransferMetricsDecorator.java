package io.github.dmitriyiliyov.springoutbox.metrics.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqTransfer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

public class OutboxDlqTransferMetricsDecorator implements OutboxDlqTransfer {

    private final OutboxDlqTransfer delegate;
    private final Timer timerTo;
    private final Timer timerFrom;

    public OutboxDlqTransferMetricsDecorator(MeterRegistry registry, OutboxDlqTransfer delegate) {
        this.timerTo = registry.timer("outbox_dlq_transfer_to_duration");
        this.timerFrom = registry.timer("outbox_dlq_transfer_from_duration");
        this.delegate = delegate;
    }

    @Override
    public void transferToDlq(int batchSize) {
        timerTo.record(() -> delegate.transferToDlq(batchSize));
    }

    @Override
    public void transferFromDlq(int batchSize) {
        timerFrom.record(() -> delegate.transferFromDlq(batchSize));
    }
}
