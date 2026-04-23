package io.github.dmitriyiliyov.springoutbox.starter;

public enum PollingType {
    FIXED, ADAPTIVE;

    public static PollingType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PoolingType is null, empty or blank");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported pooling type '%s'".formatted(value));
        }
    }
}
