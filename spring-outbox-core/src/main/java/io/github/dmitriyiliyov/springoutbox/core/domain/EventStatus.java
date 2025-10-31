package io.github.dmitriyiliyov.springoutbox.core.domain;

public enum EventStatus {
    PENDING, IN_PROCESS, PROCESSED, FAILED;

    public static EventStatus fromString(String value) {
        if (value == null) {
            return PENDING;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown EventStatus: " + value);
        }
    }
}
