package io.github.dmitriyiliyov.springoutbox.core.dlq;

import java.util.Objects;

public enum DlqStatus {
    NEW, IN_PROCESS, RESOLVED;

    public static DlqStatus froString(String value) {
        Objects.requireNonNull(value, "value cannot be null");
        try {
            return valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown DlqStatus");
        }
    }
}
