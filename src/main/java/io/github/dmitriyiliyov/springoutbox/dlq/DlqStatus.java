package io.github.dmitriyiliyov.springoutbox.dlq;

import java.util.Objects;

public enum DlqStatus {
    NEW, IN_PROCESS, RESOLVED, TO_RETRY;

    public static DlqStatus fromString(String value) {
        Objects.requireNonNull(value, "value cannot be null");
        try {
            return valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown DlqStatus");
        }
    }
}
