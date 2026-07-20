package io.github.dmitriyiliyov.oncebox.starter;

public enum TransportType {
    KAFKA, RABBIT;

    public static TransportType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TransportType is null or blank");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown TransportType: " + value);
        }
    }
}
