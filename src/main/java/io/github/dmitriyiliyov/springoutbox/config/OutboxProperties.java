package io.github.dmitriyiliyov.springoutbox.config;

import io.github.dmitriyiliyov.springoutbox.core.domain.SenderType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "outbox")
public class OutboxProperties {

    private static final int DEFAULT_THREAD_POOL_SIZE = Math.min(Runtime.getRuntime().availableProcessors(), 5);
    private final SenderProperties sender;
    private final Defaults defaults;
    private final Map<String, EventProperties> events;
    private final CleanUpProperties cleanUp;
    private final Integer threadPoolSize;

    @ConstructorBinding
    public OutboxProperties(SenderProperties sender,
                            Defaults defaults,
                            Map<String, EventProperties> events,
                            CleanUpProperties cleanUp,
                            Integer threadPoolSize) {
        this.sender = Objects.requireNonNull(sender, "sender cannot be null");
        this.defaults = defaults == null ? new Defaults() : defaults;
        Objects.requireNonNull(events, "events cannot be null");
        this.events = applyDefaults(events);
        this.cleanUp = cleanUp;
        this.threadPoolSize = threadPoolSize == null ? DEFAULT_THREAD_POOL_SIZE : threadPoolSize;
    }

    private Map<String, EventProperties> applyDefaults(Map<String, EventProperties> eventPropertiesMap) {
        return eventPropertiesMap.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        e -> {
                            String eventType = e.getKey();
                            Objects.requireNonNull(eventType, "eventType cannot be null");
                            if (eventType.isBlank()) {
                                throw new IllegalArgumentException("Topic cannot be blank");
                            }
                            return eventType;
                        },
                        e -> {
                            EventProperties event = e.getValue();
                            Integer batchSize = event.batchSize == null ? defaults.batchSize : event.batchSize;
                            Duration initialDelay = event.initialDelay == null ? defaults.initialDelay : event.initialDelay;
                            Duration fixedDelay = event.fixedDelay == null ? defaults.fixedDelay : event.fixedDelay;
                            Integer maxRetries = event.maxRetries == null ? defaults.maxRetries : event.maxRetries;
                            return new EventProperties(e.getKey(), event.topic, batchSize, initialDelay, fixedDelay, maxRetries);
                        }
                ));
    }

    public SenderProperties getSender() {
        return sender;
    }

    public Map<String, EventProperties> getEvents() {
        return events;
    }

    public CleanUpProperties getCleanUp() {
        return cleanUp;
    }

    public boolean existEventType(String eventType) {
        return events.containsKey(eventType);
    }

    public Integer getThreadPoolSize() {
        return threadPoolSize;
    }

    public record SenderProperties(SenderType type, String beanName) {
            public SenderProperties(SenderType type, String beanName) {
                this.type = Objects.requireNonNull(type, "senderType cannot be null");
                Objects.requireNonNull(beanName, "beanName cannot be null");
                if (beanName.isBlank()) {
                    throw new IllegalArgumentException("BeanName cannot be blank");
                }
                this.beanName = beanName;
            }
    }

    public static class Defaults {

        private static final int DEFAULT_BATCH_SIZE = 50;
        private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofSeconds(1800);
        private static final Duration DEFAULT_FIXED_DELAY = Duration.ofSeconds(1);
        private static final int DEFAULT_MAX_RETRY = 3;

        private final Integer batchSize;
        private final Duration initialDelay;
        private final Duration fixedDelay;
        private final Integer maxRetries;

        public Defaults() {
            this.batchSize = DEFAULT_BATCH_SIZE;
            this.initialDelay = DEFAULT_INITIAL_DELAY;
            this.fixedDelay = DEFAULT_FIXED_DELAY;
            this.maxRetries = DEFAULT_MAX_RETRY;
        }

        public Defaults(Integer batchSize, Duration initialDelay, Duration fixedDelay, Integer maxRetries) {
            this.batchSize = batchSize == null || batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
            this.initialDelay = initialDelay == null ? DEFAULT_INITIAL_DELAY : initialDelay;
            this.fixedDelay = fixedDelay == null ? DEFAULT_FIXED_DELAY : fixedDelay;
            this.maxRetries = maxRetries == null || maxRetries < 0 ? DEFAULT_MAX_RETRY : maxRetries;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public Duration getInitialDelay() {
            return initialDelay;
        }

        public Duration getFixedDelay() {
            return fixedDelay;
        }

        public int getMaxRetries() {
            return maxRetries;
        }
    }

    public record EventProperties(String eventType, String topic, Integer batchSize, Duration initialDelay,
                                  Duration fixedDelay, Integer maxRetries) {
        public EventProperties {
            Objects.requireNonNull(topic, "topic cannot be null");
            if (topic.isBlank()) {
                throw new IllegalArgumentException("Topic cannot be blank");
            }
        }
    }

    public static class CleanUpProperties {

        private static final int DEFAULT_BATCH_SIZE = 50;
        private static final Duration DEFAULT_TTL = Duration.ofHours(24);
        private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofSeconds(1800);
        private static final Duration DEFAULT_FIXED_DELAY = Duration.ofSeconds(300);

        private final Boolean enabled;
        private final Integer batchSize;
        private final Duration ttl;
        private final Duration initialDelay;
        private final Duration fixedDelay;

        public CleanUpProperties(Boolean enabled, Integer batchSize, Duration ttl, Duration initialDelay, Duration fixedDelay) {
            this.enabled = enabled;
            if (enabled == null || enabled) {
                this.batchSize = batchSize == null || batchSize < 0 ? DEFAULT_BATCH_SIZE : batchSize;
                this.ttl = ttl == null ? DEFAULT_TTL : ttl;
                this.initialDelay = initialDelay == null ? DEFAULT_INITIAL_DELAY : initialDelay;
                this.fixedDelay = fixedDelay == null ? DEFAULT_FIXED_DELAY : fixedDelay;
            } else {
                this.batchSize = 0;
                this.ttl = null;
                this.initialDelay =  null;
                this.fixedDelay =  null;
            }
        }

        public boolean isEnabled() {
            return enabled;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public Duration getTtl() {
            return ttl;
        }

        public Duration getInitialDelay() {
            return initialDelay;
        }

        public Duration getFixedDelay() {
            return fixedDelay;
        }
    }
}