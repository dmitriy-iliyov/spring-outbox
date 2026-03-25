package io.github.dmitriyiliyov.springoutbox.core.it.conteiners;

import org.testcontainers.containers.GenericContainer;

public class RedisTestContainerSingleton {

    public static final GenericContainer<?> INSTANCE;

    static {
        INSTANCE = new GenericContainer<>("redis:7")
                .withExposedPorts(6379);
        INSTANCE.start();
    }

    private RedisTestContainerSingleton() {}
}
