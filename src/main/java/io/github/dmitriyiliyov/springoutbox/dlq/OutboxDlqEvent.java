package io.github.dmitriyiliyov.springoutbox.dlq;

import io.github.dmitriyiliyov.springoutbox.core.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;

import java.time.Instant;
import java.util.UUID;

public class OutboxDlqEvent extends OutboxEvent {

    private final DlqStatus dlqStatus;

    public OutboxDlqEvent(UUID id, EventStatus status, String eventType, String payloadType, String payload,
                          int retryCount, Instant createdAt, Instant updatedAt, DlqStatus dlqStatus) {
        super(id, status, eventType, payloadType, payload, retryCount, createdAt, updatedAt);
        this.dlqStatus = dlqStatus;
    }

    public DlqStatus getDlqStatus() {
        return dlqStatus;
    }
}