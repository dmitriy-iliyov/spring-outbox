package io.github.dmitriyiliyov.springoutbox.tests.e2e.config;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public final class KafkaContainerSingleton {

    public static final KafkaContainer INSTANCE;

    static {
        INSTANCE = new KafkaContainer(DockerImageName.parse("apache/kafka:4.2.0"));
        INSTANCE.setPortBindings(List.of("39092:9092"));
        INSTANCE.start();
    }

    private KafkaContainerSingleton() {}

    /**
     * Stops the broker process while keeping the container definition (and its fixed port binding)
     * alive, so the broker can be brought back with the same bootstrap servers.
     */
    public static void stopBroker() {
        INSTANCE.getDockerClient()
                .stopContainerCmd(INSTANCE.getContainerId())
                .exec();
        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .until(() -> !isBrokerReachable());
    }

    public static void startBroker() {
        if (!isContainerRunning()) {
            INSTANCE.getDockerClient()
                    .startContainerCmd(INSTANCE.getContainerId())
                    .exec();
        }
        await().atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofMillis(500))
                .until(KafkaContainerSingleton::isBrokerReachable);
    }

    /**
     * A raw TCP connect is not a valid readiness check here: the Docker port forwarder accepts
     * connections right after docker start, long before the broker inside is actually serving.
     * Only a successful Kafka API call proves the broker is back.
     */
    public static boolean isBrokerReachable() {
        Map<String, Object> props = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, INSTANCE.getBootstrapServers(),
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000,
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 2000
        );
        try (AdminClient client = AdminClient.create(props)) {
            client.describeCluster().nodes().get(2, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isContainerRunning() {
        InspectContainerResponse response = INSTANCE.getDockerClient()
                .inspectContainerCmd(INSTANCE.getContainerId())
                .exec();
        return Boolean.TRUE.equals(response.getState().getRunning());
    }
}
