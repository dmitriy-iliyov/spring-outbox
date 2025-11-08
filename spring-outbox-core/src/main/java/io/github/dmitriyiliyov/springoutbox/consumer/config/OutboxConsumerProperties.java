package io.github.dmitriyiliyov.springoutbox.consumer.config;

import io.github.dmitriyiliyov.springoutbox.config.CleanUpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class OutboxConsumerProperties {

    private static final Logger log = LoggerFactory.getLogger(OutboxConsumerProperties.class);

    private boolean enabled;
    @NestedConfigurationProperty
    private CleanUpProperties cleanUp;

    public OutboxConsumerProperties() {}

    public void initialize() {
        if (cleanUp != null) {
            cleanUp.initialize();
        } else {
            log.warn("Outbox is configured with disabled clean-up, outbox storage will not be cleaned automatically.");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public CleanUpProperties getCleanUp() {
        return cleanUp;
    }

    public void setCleanUp(CleanUpProperties cleanUp) {
        this.cleanUp = cleanUp;
    }
}
