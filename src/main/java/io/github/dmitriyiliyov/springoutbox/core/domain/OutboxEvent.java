package io.github.dmitriyiliyov.springoutbox.core.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class OutboxEvent {

    private final UUID id;
    private OutboxStatus status;
    private final String eventType;
    private final String payloadType;
    private final String payload;
    private int retryCount;
    private final Instant createdAt;
    private Instant processedAt;

    public OutboxEvent(UUID id, String eventType, String payloadType, String payload) {
        this.id = id;
        this.status = OutboxStatus.PENDING;
        this.eventType = eventType;
        this.payloadType = payloadType;
        this.payload = payload;
        this.retryCount = 0;
        this.createdAt = Instant.now();
        this.processedAt = null;
    }

    public OutboxEvent(UUID id, OutboxStatus status, String eventType, String payloadType, String payload, int retryCount, Instant createdAt, Instant processedAt) {
        this.id = id;
        this.status = status;
        this.eventType = eventType;
        this.payloadType = payloadType;
        this.payload = payload;
        this.retryCount = retryCount;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutboxEvent that = (OutboxEvent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public UUID getId() {
        return id;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayloadType() {
        return payloadType;
    }

    public String getPayload() {
        return payload;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
