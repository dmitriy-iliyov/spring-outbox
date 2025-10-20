package io.github.dmitriyiliyov.springoutbox.core.dlq;

import io.github.dmitriyiliyov.springoutbox.core.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.stream.Collectors;

public final class DefaultDlqTransfer implements DlqTransfer {

    private static final Logger log = LoggerFactory.getLogger(DefaultDlqTransfer.class);
    private final OutboxManager manager;
    private final OutboxDlqHandler handler;
    private final OutboxDlqManager dlqManager;
    private final TransactionTemplate transactionTemplate;

    public DefaultDlqTransfer(OutboxManager manager, OutboxDlqHandler handler, OutboxDlqManager dlqManager,
                              TransactionTemplate transactionTemplate) {
        this.manager = manager;
        this.handler = handler;
        this.dlqManager = dlqManager;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void transferBatch(int batchSize) {
        List<OutboxEvent> events = manager.loadBatch(EventStatus.FAILED, batchSize, "failed_at");
        if (events == null || events.isEmpty()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            try {
                dlqManager.saveBatch(toDlqEvents(events));
                manager.deleteBatch(
                        events.stream()
                                .map(OutboxEvent::getId)
                                .collect(Collectors.toSet())
                );
            } catch (Exception e) {
                log.error("Error when transferring event batch to DLQ ", e);
                throw e;
            }
        });
        handler.handle(events);
    }

    private OutboxDlqEvent toDlqEvent(OutboxEvent event) {
        return new OutboxDlqEvent(
                event.getId(),
                event.getStatus(),
                event.getEventType(),
                event.getPayloadType(),
                event.getPayload(),
                event.getRetryCount(),
                event.getCreatedAt(),
                event.getProcessedAt(),
                event.getFailedAt(),
                DlqStatus.NEW
        );
    }

    private List<OutboxDlqEvent> toDlqEvents(List<OutboxEvent> events) {
        return events.stream()
                .map(this::toDlqEvent)
                .toList();
    }
}