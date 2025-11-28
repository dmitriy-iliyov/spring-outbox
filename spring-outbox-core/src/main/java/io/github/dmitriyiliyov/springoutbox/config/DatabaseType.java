package io.github.dmitriyiliyov.springoutbox.config;

public enum DatabaseType {
    POSTGRESQL, MYSQL, ORACLE;

    public static DatabaseType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DatabaseType is null, empty or blank");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported database '%s'".formatted(value));
        }
    }
}
