package io.github.dmitriyiliyov.oncebox.tests.e2e.config;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;

public final class RabbitContainerSingleton {

    private static final int FIXED_AMQP_PORT = 35672;

    public static final RabbitMQContainer INSTANCE;

    static {
        INSTANCE = new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.0-management"));
        // A fixed host port is required for broker outage scenarios: docker start reassigns ephemeral
        // host ports, which would invalidate the connection settings cached by clients
        INSTANCE.setPortBindings(List.of(FIXED_AMQP_PORT + ":5672"));
        INSTANCE.start();
    }

    private RabbitContainerSingleton() {}

    public static String getHost() {
        return INSTANCE.getHost();
    }

    public static int getAmqpPort() {
        return FIXED_AMQP_PORT;
    }

    public static String getUsername() {
        return INSTANCE.getAdminUsername();
    }

    public static String getPassword() {
        return INSTANCE.getAdminPassword();
    }

    /**
     * Stops the broker process while keeping the container definition (and its fixed port binding)
     * alive, so the broker can be brought back with the same connection settings.
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
                .until(RabbitContainerSingleton::isBrokerReachable);
    }

    /**
     * A raw TCP connect is not a valid readiness check: the Docker port forwarder accepts connections
     * right after docker start, long before the broker inside is serving. Only a real AMQP handshake proves it.
     */
    public static boolean isBrokerReachable() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(getHost());
        factory.setPort(getAmqpPort());
        factory.setUsername(getUsername());
        factory.setPassword(getPassword());
        factory.setConnectionTimeout(1000);
        try (Connection connection = factory.newConnection()) {
            return connection.isOpen();
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
