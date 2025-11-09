package io.github.dmitriyiliyov.springoutbox.publisher.domain;

public enum OutboxConstants {
    EVENT_ID_HEADER("event_id");

    private final String value;

    OutboxConstants(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
