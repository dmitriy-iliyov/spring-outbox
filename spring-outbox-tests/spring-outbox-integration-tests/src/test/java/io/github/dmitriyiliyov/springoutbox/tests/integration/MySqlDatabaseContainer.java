package io.github.dmitriyiliyov.springoutbox.tests.integration;

import io.github.dmitriyiliyov.springoutbox.tests.utils.MySqlTestContainerSingleton;
import org.testcontainers.containers.MySQLContainer;

public class MySqlDatabaseContainer implements DatabaseContainer {

    private final MySQLContainer<?> container = MySqlTestContainerSingleton.INSTANCE;

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.MY_SQL;
    }

    @Override
    public void start() {
        container.start();
    }

    @Override
    public void stop() {
        container.stop();
    }

    @Override
    public String getJdbcUrl() {
        return container.getJdbcUrl();
    }

    @Override
    public String getUsername() {
        return container.getUsername();
    }

    @Override
    public String getPassword() {
        return container.getPassword();
    }
}
