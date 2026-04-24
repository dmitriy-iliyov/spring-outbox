package io.github.dmitriyiliyov.springoutbox.tests.integration.consume.rabbit;

import org.testcontainers.containers.RabbitMQContainer;

public class RabbitTestContainerSingleton {

    public static final RabbitMQContainer INSTANCE;

    static {
        INSTANCE = new RabbitMQContainer("rabbitmq:4.0-management")
                .withReuse(true);
        INSTANCE.start();
    }

    private RabbitTestContainerSingleton() {}
}