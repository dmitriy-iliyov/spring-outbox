package io.github.dmitriyiliyov.springoutbox.publisher.domain;

public enum OutboxHeaders {
    EVENT_TYPE("outbox_event_type"),
    EVENT_ID("outbox_event_id");

    private final String value;

    OutboxHeaders(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
