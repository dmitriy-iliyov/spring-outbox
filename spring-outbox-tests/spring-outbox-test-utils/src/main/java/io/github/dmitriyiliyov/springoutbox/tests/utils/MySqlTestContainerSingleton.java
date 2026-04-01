package io.github.dmitriyiliyov.springoutbox.tests.utils;

import org.testcontainers.containers.MySQLContainer;

public class MySqlTestContainerSingleton {

    public static final MySQLContainer<?> INSTANCE;

    static {
        INSTANCE = new MySQLContainer<>("mysql:8")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
        INSTANCE.start();
    }

    private MySqlTestContainerSingleton() {}
}
