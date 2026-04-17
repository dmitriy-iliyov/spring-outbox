package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultOutboxDlqTransfer implements OutboxDlqTransfer {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxDlqTransfer.class);

    private final TransactionTemplate transactionTemplate;
    private final OutboxManager manager;
    private final OutboxDlqManager dlqManager;
    private final OutboxDlqEventMapper eventMapper;
    private final OutboxDlqHandler handler;

    public DefaultOutboxDlqTransfer(TransactionTemplate transactionTemplate,
                                    OutboxManager manager,
                                    OutboxDlqManager dlqManager,
                                    OutboxDlqEventMapper eventMapper,
                                    OutboxDlqHandler handler) {
        this.transactionTemplate = transactionTemplate;
        this.manager = manager;
        this.dlqManager = dlqManager;
        this.eventMapper = eventMapper;
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
                dlqManager.saveBatch(eventMapper.toDlqEvents(events));
                manager.deleteBatch(
                        events.stream()
                                .map(OutboxEvent::getId)
                                .collect(Collectors.toSet())
                );
            } catch (Exception e) {
                log.error("Error when transferring events from outbox to DLQ", e);
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
                manager.saveBatch(eventMapper.toOutboxEvents(dlqEvents));
                dlqManager.deleteBatch(
                        dlqEvents.stream()
                                .map(OutboxDlqEvent::getId)
                                .collect(Collectors.toSet())
                );
            } catch (Exception e) {
                log.error("Error when transferring events from DLQ to outbox", e);
                throw e;
            }
        });
    }
}