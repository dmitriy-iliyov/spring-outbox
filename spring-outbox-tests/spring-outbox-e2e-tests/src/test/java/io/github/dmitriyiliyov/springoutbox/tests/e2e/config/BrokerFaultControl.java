package io.github.dmitriyiliyov.springoutbox.tests.e2e.config;

/**
 * Broker-agnostic fault injection facade: stops/starts the container of the active broker only,
 * so referencing this never boots the other broker's singleton.
 */
public final class BrokerFaultControl {

    private BrokerFaultControl() {}

    public static void stopBroker() {
        switch (BrokerType.current()) {
            case KAFKA -> KafkaContainerSingleton.stopBroker();
            case RABBIT -> RabbitContainerSingleton.stopBroker();
        }
    }

    public static void startBroker() {
        switch (BrokerType.current()) {
            case KAFKA -> KafkaContainerSingleton.startBroker();
            case RABBIT -> RabbitContainerSingleton.startBroker();
        }
    }
}
