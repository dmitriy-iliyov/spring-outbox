package io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.config.containers;

import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

public class KafkaTestContainerSingleton {

    public static final KafkaContainer INSTANCE;

    static {
        INSTANCE = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"))
                .withReuse(true);
        INSTANCE.start();
    }

    private KafkaTestContainerSingleton() {}
}