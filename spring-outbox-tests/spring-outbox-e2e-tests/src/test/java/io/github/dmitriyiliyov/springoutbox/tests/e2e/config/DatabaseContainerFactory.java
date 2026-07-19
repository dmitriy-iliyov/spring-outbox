package io.github.dmitriyiliyov.springoutbox.tests.e2e.config;

public final class DatabaseContainerFactory {

    private static final String DB = System.getProperty("db", "postgres");
    public static final DatabaseContainer DB_CONTAINER = generate();

    static {
        DB_CONTAINER.start();
    }

    private DatabaseContainerFactory() {}

    private static DatabaseContainer generate() {
        return switch (DB) {
            case "postgres" -> new PostgresDatabaseContainer();
            default -> throw new UnsupportedOperationException(
                    "E2E infrastructure for db='" + DB + "' is not implemented yet"
            );
        };
    }
}