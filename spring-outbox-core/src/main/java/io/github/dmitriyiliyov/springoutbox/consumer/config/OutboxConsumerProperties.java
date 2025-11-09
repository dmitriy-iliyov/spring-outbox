package io.github.dmitriyiliyov.springoutbox.consumer.config;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;
import java.util.Objects;

public class OutboxConsumerProperties {

    private static final Logger log = LoggerFactory.getLogger(OutboxConsumerProperties.class);

    private Boolean enabled;
    @NestedConfigurationProperty
    private OutboxProperties.CleanUpProperties cleanUp;
    @NestedConfigurationProperty
    private CacheProperties cache;

    public void initialize() {
        if (enabled != null && enabled) {
            if (cleanUp == null) {
                cleanUp = new OutboxProperties.CleanUpProperties();
                cleanUp.setEnabled(true);
            }
            cleanUp.initialize();
            if (!cleanUp.isEnabled()) {
                log.warn("Consumer Outbox is configured with disabled clean-up, consumed outbox storage will not be cleaned automatically");
            }
            if (cache == null) {
                cache = new CacheProperties();
                cache.setEnabled(true);
            }
            cache.initialize();
            if (!cache.isEnabled()) {
                log.warn("Consumer Outbox is configured with disabled cache");
            }
        } else {
            enabled = false;
        }
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public OutboxProperties.CleanUpProperties getCleanUp() {
        return cleanUp;
    }

    public void setCleanUp(OutboxProperties.CleanUpProperties cleanUp) {
        this.cleanUp = cleanUp;
    }

    public CacheProperties getCache() {
        return cache;
    }

    public void setCache(CacheProperties cache) {
        this.cache = cache;
    }

    public static final class CacheProperties {

        public Boolean enabled;
        public String cacheName;

        public void initialize() {
            if (enabled != null && enabled) {
                enabled = true;
                Objects.requireNonNull(cacheName, "cacheName cannot be null");
                if (cacheName.isBlank()) {
                    throw new IllegalArgumentException("cacheName cannot be empty or blank");
                }
            } else {
                enabled = false;
                cacheName = null;
            }
        }

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getCacheName() {
            return cacheName;
        }

        public void setCacheName(String cacheName) {
            this.cacheName = cacheName;
        }
    }
}
