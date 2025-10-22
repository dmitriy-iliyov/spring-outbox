package io.github.dmitriyiliyov.springoutbox.dlq;

import io.github.dmitriyiliyov.springoutbox.core.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.stream.Collectors;

public final class DefaultOutboxDlqTransfer implements OutboxDlqTransfer {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxDlqTransfer.class);

    private final TransactionTemplate transactionTemplate;
    private final OutboxManager manager;
    private final OutboxDlqManager dlqManager;
    private final OutboxDlqHandler handler;

    public DefaultOutboxDlqTransfer(TransactionTemplate transactionTemplate, OutboxManager manager,
                                    OutboxDlqManager dlqManager, OutboxDlqHandler handler) {
        this.transactionTemplate = transactionTemplate;
        this.manager = manager;
        this.dlqManager = dlqManager;
        this.handler = handler;
    }

    @Override
    public void transferOutboxToDlq(int batchSize) {
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
                log.error("Error when transferring events from Outbox to DLQ", e);
                throw e;
            }
        });
        handler.handle(events);
    }

    @Override
    public void transferDlqToOutbox(int batchSize) {
        List<OutboxDlqEvent> dlqEvents = dlqManager.loadBatch(DlqStatus.TO_RETRY, batchSize);
        if (dlqEvents == null || dlqEvents.isEmpty()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            try {
                manager.saveBatch(toOutboxEvents(dlqEvents));
                dlqManager.deleteBatch(
                        dlqEvents.stream()
                        .map(OutboxDlqEvent::getId)
                        .collect(Collectors.toSet())
                );
            } catch (Exception e) {
                log.error("Error when transferring events from DLQ to Outbox", e);
                throw e;
            }
        });
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

    private OutboxEvent toOutboxEvent(OutboxDlqEvent event) {
        return new OutboxEvent(
                event.getId(),
                event.getStatus(),
                event.getEventType(),
                event.getPayloadType(),
                event.getPayload(),
                event.getRetryCount(),
                event.getCreatedAt(),
                event.getProcessedAt(),
                event.getFailedAt()
        );
    }

    private List<OutboxEvent> toOutboxEvents(List<OutboxDlqEvent> events) {
        return events.stream()
                .map(this::toOutboxEvent)
                .toList();
    }
}