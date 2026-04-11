package io.github.dmitriyiliyov.springoutbox.tests.integration.consume.rabbitmq;

import org.testcontainers.containers.RabbitMQContainer;

public class RabbitMqTestContainerSingleton {

    public static final RabbitMQContainer INSTANCE;

    static {
        INSTANCE = new RabbitMQContainer("rabbitmq:4.0-management")
                .withReuse(true);
        INSTANCE.start();
    }

    private RabbitMqTestContainerSingleton() {}
}