package io.github.dmitriyiliyov.springoutbox.tests.utils;

import org.testcontainers.containers.GenericContainer;

public class RedisTestContainerSingleton {

    public static final GenericContainer<?> INSTANCE;

    static {
        INSTANCE = new GenericContainer<>("redis:7")
                .withExposedPorts(6379)
                .withReuse(true);
        INSTANCE.start();
    }

    private RedisTestContainerSingleton() {}
}
