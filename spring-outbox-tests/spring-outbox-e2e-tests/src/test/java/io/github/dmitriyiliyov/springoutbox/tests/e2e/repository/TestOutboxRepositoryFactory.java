package io.github.dmitriyiliyov.springoutbox.tests.e2e.repository;

import io.github.dmitriyiliyov.springoutbox.tests.e2e.config.DatabaseType;
import org.springframework.jdbc.core.JdbcTemplate;

public final class TestOutboxRepositoryFactory {

    private TestOutboxRepositoryFactory() {}

    public static TestOutboxRepository generate(DatabaseType databaseType, JdbcTemplate jdbcTemplate) {
        return switch (databaseType) {
            case POSTGRES_SQL -> new PostgresTestOutboxRepository(jdbcTemplate);
            default -> throw new UnsupportedOperationException(
                    "TestOutboxRepository for databaseType=" + databaseType + " is not implemented yet"
            );
        };
    }
}
