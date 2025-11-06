package io.github.dmitriyiliyov.springoutbox.config;

public enum SenderType {
    KAFKA, RABBIT_MQ;

    public static SenderType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SenderType is null or blank");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown SenderType: " + value);
        }
    }
}
