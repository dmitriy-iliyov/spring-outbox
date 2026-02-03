package io.github.dmitriyiliyov.springoutbox.core;

import java.time.Duration;

public interface OutboxPropertiesHolder {
    interface CleanUpPropertiesHolder {
        Duration getTtl();

        Integer getBatchSize();

        Duration getInitialDelay();

        Duration getFixedDelay();
    }
}
