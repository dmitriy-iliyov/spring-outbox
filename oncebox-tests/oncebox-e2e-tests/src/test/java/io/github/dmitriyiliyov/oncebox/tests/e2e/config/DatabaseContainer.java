package io.github.dmitriyiliyov.oncebox.tests.e2e.config;

public interface DatabaseContainer {

    DatabaseType getDatabaseType();

    void start();

    void stop();

    String getJdbcUrl();

    String getUsername();

    String getPassword();
}