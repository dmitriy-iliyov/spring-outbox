package io.github.dmitriyiliyov.springoutbox.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.publisher.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
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
    public void transferToDlq(int batchSize) {
        final List<OutboxEvent> events = new ArrayList<>();
        transactionTemplate.executeWithoutResult(status -> {
            try {
                events.addAll(manager.loadBatch(EventStatus.FAILED, batchSize));
                if (events.isEmpty()) {
                    return;
                }
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
        if (!events.isEmpty()) {
            try {
                handler.handle(events);
            } catch (Exception e) {
                log.error("Error when handle events after transferring to DLQ", e);
            }
        }
    }

    @Override
    public void transferFromDlq(int batchSize) {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                List<OutboxDlqEvent> dlqEvents = dlqManager.loadAndLockBatch(DlqStatus.TO_RETRY, batchSize);
                if (dlqEvents == null || dlqEvents.isEmpty()) {
                    return;
                }
                manager.saveBatch(toOutboxEvents(dlqEvents));
                dlqManager.deleteBatch(
                        dlqEvents.stream()
                                .map(OutboxDlqEvent::getId)
                                .collect(Collectors.toSet()));
            } catch (Exception e) {
                log.error("Error when transferring events from DLQ to Outbox", e);
                throw e;
            }
        });
    }

    private OutboxDlqEvent toDlqEvent(OutboxEvent event) {
        return new OutboxDlqEvent(
                event.getId(),
                EventStatus.FAILED,
                event.getEventType(),
                event.getPayloadType(),
                event.getPayload(),
                event.getRetryCount(),
                event.getNextRetryAt(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
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
                EventStatus.PENDING,
                event.getEventType(),
                event.getPayloadType(),
                event.getPayload(),
                0,
                Instant.now(),
                event.getCreatedAt(),
                Instant.now()
        );
    }

    private List<OutboxEvent> toOutboxEvents(List<OutboxDlqEvent> events) {
        return events.stream()
                .map(this::toOutboxEvent)
                .toList();
    }
}