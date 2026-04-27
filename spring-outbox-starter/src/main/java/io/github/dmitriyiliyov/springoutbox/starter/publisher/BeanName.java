package io.github.dmitriyiliyov.springoutbox.starter.publisher;

public enum BeanName {
    STUCK_RECOVERY_SCHEDULER("outboxRecoveryScheduler"),
    CLEANUP_SCHEDULER("outboxCleanUpScheduler");

    private final String name;

    BeanName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
