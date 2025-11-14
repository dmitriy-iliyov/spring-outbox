package io.github.dmitriyiliyov.springoutbox.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.publisher.domain.OutboxEvent;

import java.time.Instant;
import java.util.UUID;

public class OutboxDlqEvent extends OutboxEvent {

    private DlqStatus dlqStatus;
    private Instant movedAt;

    public OutboxDlqEvent(UUID id, EventStatus status, String eventType, String payloadType, String payload,
                          int retryCount, Instant nextRetryAt, Instant createdAt, Instant updatedAt, DlqStatus dlqStatus,
                          Instant movedAt) {
        super(id, status, eventType, payloadType, payload, retryCount, nextRetryAt, createdAt, updatedAt);
        this.dlqStatus = dlqStatus;
        this.movedAt = movedAt;
    }

    public DlqStatus getDlqStatus() {
        return dlqStatus;
    }

    public void setDlqStatus(DlqStatus dlqStatus) {
        this.dlqStatus = dlqStatus;
    }

    public Instant getMovedAt() {
        return movedAt;
    }
}