package io.github.dmitriyiliyov.springoutbox.tests.e2e.config;

public interface DatabaseContainer {

    DatabaseType getDatabaseType();

    void start();

    void stop();

    String getJdbcUrl();

    String getUsername();

    String getPassword();
}