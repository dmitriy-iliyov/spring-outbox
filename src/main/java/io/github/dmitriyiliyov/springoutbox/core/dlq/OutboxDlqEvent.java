package io.github.dmitriyiliyov.springoutbox.core.dlq;

import io.github.dmitriyiliyov.springoutbox.core.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.domain.OutboxEvent;

import java.time.Instant;
import java.util.UUID;

public class OutboxDlqEvent extends OutboxEvent {

    private final DlqStatus dlqStatus;

    public OutboxDlqEvent(UUID id, EventStatus status, String eventType, String payloadType, String payload,
                          int retryCount, Instant createdAt, Instant processedAt, Instant failedAt, DlqStatus dlqStatus) {
        super(id, status, eventType, payloadType, payload, retryCount, createdAt, processedAt, failedAt);
        this.dlqStatus = dlqStatus;
    }

    public DlqStatus getDlqStatus() {
        return dlqStatus;
    }
}