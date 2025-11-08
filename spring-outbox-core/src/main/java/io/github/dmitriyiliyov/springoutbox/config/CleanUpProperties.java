package io.github.dmitriyiliyov.springoutbox.config;

import java.time.Duration;

public final class CleanUpProperties {

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);
    private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofSeconds(300);
    private static final Duration DEFAULT_FIXED_DELAY = Duration.ofSeconds(5);

    private Boolean enabled;
    private Integer batchSize;
    private Duration ttl;
    private Duration initialDelay;
    private Duration fixedDelay;

    public void initialize() {
        if (enabled != null && enabled) {
            enabled = true;
            batchSize = (batchSize == null || batchSize < 0) ? DEFAULT_BATCH_SIZE : batchSize;
            ttl = (ttl == null) ? DEFAULT_TTL : ttl;
            initialDelay = (initialDelay == null) ? DEFAULT_INITIAL_DELAY : initialDelay;
            fixedDelay = (fixedDelay == null) ? DEFAULT_FIXED_DELAY : fixedDelay;
        } else {
            enabled = false;
            batchSize = 0;
            ttl = null;
            initialDelay = null;
            fixedDelay = null;
        }
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public Duration getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(Duration initialDelay) {
        this.initialDelay = initialDelay;
    }

    public Duration getFixedDelay() {
        return fixedDelay;
    }

    public void setFixedDelay(Duration fixedDelay) {
        this.fixedDelay = fixedDelay;
    }
}
