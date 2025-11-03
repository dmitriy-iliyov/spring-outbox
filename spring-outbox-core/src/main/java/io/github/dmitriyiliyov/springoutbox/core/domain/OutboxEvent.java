package io.github.dmitriyiliyov.springoutbox.core.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class OutboxEvent {

    protected final UUID id;
    protected final EventStatus status;
    protected final String eventType;
    protected final String payloadType;
    protected final String payload;
    protected final int retryCount;
    protected final Instant nextRetryAt;
    protected final Instant createdAt;
    protected final Instant updatedAt;

    public OutboxEvent(UUID id, String eventType, String payloadType, String payload) {
        this.id = id;
        this.status = EventStatus.PENDING;
        this.eventType = eventType;
        this.payloadType = payloadType;
        this.payload = payload;
        this.retryCount = -1;
        this.nextRetryAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public OutboxEvent(UUID id, EventStatus status, String eventType, String payloadType, String payload, int retryCount,
                       Instant nextRetryAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.status = status;
        this.eventType = eventType;
        this.payloadType = payloadType;
        this.payload = payload;
        this.retryCount = retryCount;
        this.nextRetryAt = nextRetryAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public EventStatus getStatus() {
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

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
