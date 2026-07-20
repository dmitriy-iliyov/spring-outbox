package io.github.dmitriyiliyov.oncebox.tests.integration;

public final class DatabaseContainerFactory {

    private static final String DB = System.getProperty("db", "postgres");
    public static final DatabaseContainer DB_CONTAINER = generate();

    static {
        DB_CONTAINER.start();
    }

    private static DatabaseContainer generate() {
        return switch (DB) {
            case "mysql" -> new MySqlDatabaseContainer();
            case "oracle" -> new OracleDatabaseContainer();
            default -> new PostgresDatabaseContainer();
        };
    }
}
