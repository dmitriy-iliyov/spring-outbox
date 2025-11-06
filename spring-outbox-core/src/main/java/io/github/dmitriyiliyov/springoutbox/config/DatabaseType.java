package io.github.dmitriyiliyov.springoutbox.config;

public enum DatabaseType {
    POSTGRESQL, MYSQL, ORACLE;

    public static DatabaseType fromString(String value) {
        if (value.isBlank()) {
            throw new IllegalArgumentException("DatabaseType is null or blank");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported DatabaseType");
        }
    }
}
