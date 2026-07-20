package io.github.dmitriyiliyov.oncebox.core.publisher.domain;

public enum OutboxHeaders {
    EVENT_TYPE("outbox_event_type"),
    EVENT_ID("outbox_event_id"),
    EVENT_PAYLOAD_TYPE("outbox_event_payload_type");

    private final String value;

    OutboxHeaders(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
