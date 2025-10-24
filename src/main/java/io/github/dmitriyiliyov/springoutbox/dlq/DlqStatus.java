package io.github.dmitriyiliyov.springoutbox.dlq;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

public enum DlqStatus {
    NEW, IN_PROCESS, RESOLVED, TO_RETRY;

    @JsonCreator
    public static DlqStatus fromString(String value) {
        Objects.requireNonNull(value, "value cannot be null");
        try {
            return valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown DlqStatus");
        }
    }
}
