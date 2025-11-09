package io.github.dmitriyiliyov.springoutbox.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxEvent;

import java.time.Instant;
import java.util.UUID;

public class OutboxDlqEvent extends OutboxEvent {

    private final DlqStatus dlqStatus;

    public OutboxDlqEvent(UUID id, EventStatus status, String eventType, String payloadType, String payload,
                          int retryCount, Instant nextRetryAt, Instant createdAt, Instant updatedAt, DlqStatus dlqStatus) {
        super(id, status, eventType, payloadType, payload, retryCount, nextRetryAt, createdAt, updatedAt);
        this.dlqStatus = dlqStatus;
    }

    public DlqStatus getDlqStatus() {
        return dlqStatus;
    }
}