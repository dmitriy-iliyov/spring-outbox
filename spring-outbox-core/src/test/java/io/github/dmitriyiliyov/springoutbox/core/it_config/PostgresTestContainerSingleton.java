package io.github.dmitriyiliyov.springoutbox.core.it_config;

import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresTestContainerSingleton {

    public static final PostgreSQLContainer<?> INSTANCE;

    static {
        INSTANCE = new PostgreSQLContainer<>("postgres:16")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
        INSTANCE.start();
    }

    private PostgresTestContainerSingleton() {}
}