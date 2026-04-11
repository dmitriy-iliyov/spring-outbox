package io.github.dmitriyiliyov.springoutbox.tests.integration;

public interface DatabaseContainer {

    DatabaseType getDatabaseType();

    void start();

    void stop();

    String getJdbcUrl();

    String getUsername();

    String getPassword();
}
