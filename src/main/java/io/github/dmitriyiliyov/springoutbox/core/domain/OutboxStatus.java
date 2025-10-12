package io.github.dmitriyiliyov.springoutbox.core.domain;

public enum OutboxStatus {
    PENDING, IN_PROCESS, PROCESSED, FAILED;

    public static OutboxStatus fromString(String value) {
        if (value == null) {
            return PENDING;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown OutboxStatus: " + value);
        }
    }
}
