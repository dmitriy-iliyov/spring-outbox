package io.github.dmitriyiliyov.springoutbox.config;

public enum SenderType {
    KAFKA, RABBIT_MQ;

    public static SenderType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("ConsumerType is null");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown ConsumerType: " + value);
        }
    }
}
