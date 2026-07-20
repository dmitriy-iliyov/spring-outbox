package io.github.dmitriyiliyov.springoutbox.tests.e2e.config;

public enum BrokerType {

    KAFKA("kafka"),
    RABBIT("rabbit");

    private final String profile;

    BrokerType(String profile) {
        this.profile = profile;
    }

    public String getProfile() {
        return profile;
    }

    /**
     * Active broker for the current JVM, chosen by the {@code -Dbroker} system property (default kafka).
     */
    public static BrokerType current() {
        String value = System.getProperty("broker", KAFKA.profile);
        return switch (value) {
            case "kafka" -> KAFKA;
            case "rabbit" -> RABBIT;
            default -> throw new IllegalArgumentException("Unknown broker='" + value + "', expected kafka or rabbit");
        };
    }
}
