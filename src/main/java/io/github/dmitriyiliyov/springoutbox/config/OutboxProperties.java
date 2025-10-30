package io.github.dmitriyiliyov.springoutbox.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "outbox")
public class OutboxProperties {

    private static final int DEFAULT_THREAD_POOL_SIZE = Math.min(Runtime.getRuntime().availableProcessors(), 5);
    private static final Logger log = LoggerFactory.getLogger(OutboxProperties.class);

    private final SenderProperties sender;
    private final Integer threadPoolSize;
    private final Defaults defaults;
    private final Map<String, EventProperties> events;
    private final StuckEventRecoveryProperties stuckEventRecovery;
    private final CleanUpProperties cleanUp;
    private final DlqProperties dlq;
    private final MigrationProperties migration;

    @ConstructorBinding
    public OutboxProperties(SenderProperties sender,
                            Integer threadPoolSize,
                            Defaults defaults,
                            Map<String, EventProperties> events,
                            StuckEventRecoveryProperties stuckEventRecovery,
                            CleanUpProperties cleanUp,
                            DlqProperties dlq,
                            MigrationProperties migration) {
        this.sender = Objects.requireNonNull(sender, "sender cannot be null");
        this.threadPoolSize = threadPoolSize == null ? DEFAULT_THREAD_POOL_SIZE : threadPoolSize;
        this.defaults = defaults == null ? new Defaults() : defaults;
        Objects.requireNonNull(events, "events cannot be null");
        if (events.isEmpty()) {
            log.warn("Outbox configuring with out events");
        }
        this.events = applyDefaults(events);
        this.stuckEventRecovery = stuckEventRecovery == null ?
                new StuckEventRecoveryProperties() : stuckEventRecovery;
        this.cleanUp = cleanUp;
        this.dlq = dlq;
        this.migration = migration == null ? new MigrationProperties() : migration;
    }

    private Map<String, EventProperties> applyDefaults(Map<String, EventProperties> eventPropertiesMap) {
        return eventPropertiesMap.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        e -> {
                            String eventType = e.getKey();
                            Objects.requireNonNull(eventType, "eventType cannot be null");
                            if (eventType.isBlank()) {
                                throw new IllegalArgumentException("Event type cannot be blank");
                            }
                            return eventType;
                        },
                        e -> {
                            EventProperties event = e.getValue();
                            Integer batchSize = event.batchSize == null ? defaults.batchSize : event.batchSize;
                            Duration initialDelay = event.initialDelay == null ? defaults.initialDelay : event.initialDelay;
                            Duration fixedDelay = event.fixedDelay == null ? defaults.fixedDelay : event.fixedDelay;
                            Integer maxRetries = event.maxRetries == null ? defaults.maxRetries : event.maxRetries;
                            BackoffProperties backoff;
                            if (event.backoff() == null) {
                                backoff = defaults.getBackoff();
                            } else if (!event.backoff().isEnabled()) {
                                backoff = new BackoffProperties(false, null, null);
                            } else {
                                backoff = new BackoffProperties(
                                        true,
                                        event.backoff().getDelay() == null ?
                                                defaults.getBackoff().getDelay() :
                                                event.backoff().getDelay(),
                                        event.backoff().getMultiplier() == null || event.backoff().getMultiplier() < 1 ?
                                                defaults.getBackoff().getMultiplier() :
                                                event.backoff().getMultiplier()
                                );
                            }
                            return new EventProperties(
                                    e.getKey(),
                                    event.topic,
                                    batchSize,
                                    initialDelay,
                                    fixedDelay,
                                    maxRetries,
                                    backoff
                            );
                        }
                ));
    }

    public SenderProperties getSender() {
        return sender;
    }

    public Integer getThreadPoolSize() {
        return threadPoolSize;
    }

    public OutboxProperties.Defaults getDefaults() {
        return this.defaults;
    }

    public Map<String, EventProperties> getEvents() {
        return events;
    }

    public StuckEventRecoveryProperties getStuckEventRecovery() {
        return stuckEventRecovery;
    }

    public boolean isCleanUpEnabled() {
        if (cleanUp == null) {
            return false;
        }
        return cleanUp.enabled();
    }

    public Optional<CleanUpProperties> getCleanUp() {
        return Optional.of(cleanUp);
    }

    public boolean existEventType(String eventType) {
        return events.containsKey(eventType);
    }

    public Optional<DlqProperties> getDlq() {
        return Optional.of(dlq);
    }

    public MigrationProperties getMigration() {
        return migration;
    }

    public record SenderProperties(SenderType type, String beanName) {
            public SenderProperties {
                Objects.requireNonNull(type, "senderType cannot be null");
                Objects.requireNonNull(beanName, "beanName cannot be null");
                if (beanName.isBlank()) {
                    throw new IllegalArgumentException("BeanName cannot be blank");
                }
            }
    }

    public static final class Defaults {

        private static final int DEFAULT_BATCH_SIZE = 50;
        private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofSeconds(300);
        private static final Duration DEFAULT_FIXED_DELAY = Duration.ofSeconds(2);
        private static final int DEFAULT_MAX_RETRY = 3;
        private static final BackoffProperties DEFAULT_BACKOFF = new BackoffProperties();

        private final Integer batchSize;
        private final Duration initialDelay;
        private final Duration fixedDelay;
        private final Integer maxRetries;
        private final BackoffProperties backoff;

        public Defaults() {
            this.batchSize = DEFAULT_BATCH_SIZE;
            this.initialDelay = DEFAULT_INITIAL_DELAY;
            this.fixedDelay = DEFAULT_FIXED_DELAY;
            this.maxRetries = DEFAULT_MAX_RETRY;
            this.backoff = DEFAULT_BACKOFF;
        }

        public Defaults(Integer batchSize, Duration initialDelay, Duration fixedDelay, Integer maxRetries, BackoffProperties backoff) {
            this.batchSize = batchSize == null || batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
            this.initialDelay = initialDelay == null ? DEFAULT_INITIAL_DELAY : initialDelay;
            this.fixedDelay = fixedDelay == null ? DEFAULT_FIXED_DELAY : fixedDelay;
            this.maxRetries = maxRetries == null || maxRetries < 0 ? DEFAULT_MAX_RETRY : maxRetries;
            this.backoff = backoff == null ? DEFAULT_BACKOFF : backoff;
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

        public BackoffProperties getBackoff() {
            return backoff;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Defaults defaults = (Defaults) o;
            return Objects.equals(batchSize, defaults.batchSize) &&
                    Objects.equals(initialDelay, defaults.initialDelay) &&
                    Objects.equals(fixedDelay, defaults.fixedDelay) &&
                    Objects.equals(maxRetries, defaults.maxRetries) &&
                    Objects.equals(backoff, defaults.backoff);
        }

        @Override
        public int hashCode() {
            return Objects.hash(batchSize, initialDelay, fixedDelay, maxRetries, backoff);
        }
    }

    public static final class BackoffProperties {

        private static final Duration DEFAULT_DELAY = Duration.ofSeconds(10);
        private static final Long DEFAULT_MULTIPLIER = 3L;

        private final Boolean enabled;
        private final Duration delay;
        private final Long multiplier;

        public BackoffProperties() {
            this.enabled = true;
            this.delay = DEFAULT_DELAY;
            this.multiplier = DEFAULT_MULTIPLIER;
        }

        public BackoffProperties(Boolean enabled, Duration delay, Long multiplier) {
            if (enabled == null || enabled) {
                this.enabled = true;
                this.delay = delay == null ? DEFAULT_DELAY : delay;
                this.multiplier = multiplier == null || multiplier < 1 ? DEFAULT_MULTIPLIER : multiplier;
            } else {
                this.enabled = false;
                this.delay = Duration.ofSeconds(0);
                this.multiplier = 1L;
            }
        }

        public Boolean isEnabled() {
            return enabled;
        }

        public Duration getDelay() {
            return delay;
        }

        public Long getMultiplier() {
            return multiplier;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BackoffProperties that = (BackoffProperties) o;
            return Objects.equals(enabled, that.enabled) &&
                    Objects.equals(delay, that.delay) &&
                    Objects.equals(multiplier, that.multiplier);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enabled, delay, multiplier);
        }
    }

    public record EventProperties(String eventType, String topic, Integer batchSize, Duration initialDelay,
                                  Duration fixedDelay, Integer maxRetries, BackoffProperties backoff) {
        public EventProperties {
            Objects.requireNonNull(eventType, "eventType cannot be null");
            if (eventType.isBlank()) {
                throw new IllegalArgumentException("Event type cannot be blank");
            }
            Objects.requireNonNull(topic, "topic cannot be null");
            if (topic.isBlank()) {
                throw new IllegalArgumentException("Topic cannot be blank");
            }
        }

        public long backoffMultiplier() {
            return backoff.getMultiplier();
        }

        public long backoffDelay() {
            return backoff.getDelay().toSeconds();
        }
    }

    public static final class StuckEventRecoveryProperties {

        private static final int DEFAULT_BATCH_SIZE = 100;
        private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofSeconds(300);
        private static final Duration DEFAULT_FIXED_DELAY = Duration.ofSeconds(1800);

        private final Integer batchSize;
        private final Duration initialDelay;
        private final Duration fixedDelay;

        public StuckEventRecoveryProperties() {
            this.batchSize = DEFAULT_BATCH_SIZE;
            this.initialDelay = DEFAULT_INITIAL_DELAY;
            this.fixedDelay = DEFAULT_FIXED_DELAY;
        }

        public StuckEventRecoveryProperties(Integer batchSize, Duration initialDelay, Duration fixedDelay) {
            this.batchSize = batchSize == null || batchSize < 0 ? DEFAULT_BATCH_SIZE : batchSize;
            this.initialDelay = initialDelay == null ? DEFAULT_INITIAL_DELAY : initialDelay;
            this.fixedDelay = fixedDelay == null ? DEFAULT_FIXED_DELAY : fixedDelay;
        }

        public Integer getBatchSize() {
            return batchSize;
        }

        public Duration getInitialDelay() {
            return initialDelay;
        }

        public Duration getFixedDelay() {
            return fixedDelay;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StuckEventRecoveryProperties that = (StuckEventRecoveryProperties) o;
            return Objects.equals(batchSize, that.batchSize) &&
                    Objects.equals(initialDelay, that.initialDelay) &&
                    Objects.equals(fixedDelay, that.fixedDelay);
        }

        @Override
        public int hashCode() {
            return Objects.hash(batchSize, initialDelay, fixedDelay);
        }
    }

    public record CleanUpProperties(Boolean enabled, Integer batchSize, Duration threshold,
                                    Duration initialDelay, Duration fixedDelay) {

        private static final int DEFAULT_BATCH_SIZE = 100;
        private static final Duration DEFAULT_THRESHOLD = Duration.ofHours(1);
        private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofSeconds(300);
        private static final Duration DEFAULT_FIXED_DELAY = Duration.ofSeconds(5);

        public CleanUpProperties(Boolean enabled, Integer batchSize, Duration threshold,
                                 Duration initialDelay, Duration fixedDelay) {
            if (enabled != null && enabled) {
                this.enabled = true;
                this.batchSize = (batchSize == null || batchSize < 0) ? DEFAULT_BATCH_SIZE : batchSize;
                this.threshold = (threshold == null) ? DEFAULT_THRESHOLD : threshold;
                this.initialDelay = (initialDelay == null) ? DEFAULT_INITIAL_DELAY : initialDelay;
                this.fixedDelay = (fixedDelay == null) ? DEFAULT_FIXED_DELAY : fixedDelay;
            } else {
                this.enabled = false;
                this.batchSize = 0;
                this.threshold = null;
                this.initialDelay = null;
                this.fixedDelay = null;
            }
        }
    }

    public record DlqProperties(Boolean enabled, Integer batchSize,
                                Duration transferToDlqInitialDelay, Duration transferToDlqFixedDelay,
                                Duration transferFromDlqInitialDelay, Duration transferFormDlqFixedDelay) {

            private static final int DEFAULT_BATCH_SIZE = 100;
            private static final Duration DEFAULT_TO_INITIAL_DELAY = Duration.ofSeconds(300);
            private static final Duration DEFAULT_TO_FIXED_DELAY = Duration.ofSeconds(900);
            private static final Duration DEFAULT_FROM_INITIAL_DELAY = Duration.ofSeconds(300);
            private static final Duration DEFAULT_FROM_FIXED_DELAY = Duration.ofSeconds(3600);

            public DlqProperties(Boolean enabled, Integer batchSize, Duration transferToDlqInitialDelay, Duration transferToDlqFixedDelay,
                                 Duration transferFromDlqInitialDelay, Duration transferFormDlqFixedDelay) {
                if (enabled != null && enabled) {
                    this.enabled = true;
                    this.batchSize = batchSize == null || batchSize < 0 ? DEFAULT_BATCH_SIZE : batchSize;
                    this.transferToDlqInitialDelay = transferToDlqInitialDelay == null ? DEFAULT_TO_INITIAL_DELAY : transferToDlqInitialDelay;
                    this.transferToDlqFixedDelay = transferToDlqFixedDelay == null ? DEFAULT_TO_FIXED_DELAY : transferToDlqFixedDelay;
                    this.transferFromDlqInitialDelay = transferFromDlqInitialDelay == null ? DEFAULT_FROM_INITIAL_DELAY : transferFromDlqInitialDelay;
                    this.transferFormDlqFixedDelay = transferFormDlqFixedDelay == null ? DEFAULT_FROM_FIXED_DELAY : transferFormDlqFixedDelay;
                } else {
                    this.enabled = false;
                    this.batchSize = 0;
                    this.transferToDlqInitialDelay = null;
                    this.transferToDlqFixedDelay = null;
                    this.transferFromDlqInitialDelay = null;
                    this.transferFormDlqFixedDelay = null;
                }
            }
    }

    public static final class MigrationProperties {

        private static final String DEFAULT_LOCATION = "classpath:db/migration/outbox";
        private static final String DEFAULT_TABLE = "outbox_schema_history";

        private final Boolean enabled;
        private final String location;
        private final String table;

        public MigrationProperties() {
            this.enabled = true;
            this.location = DEFAULT_LOCATION;
            this.table = DEFAULT_TABLE;
        }

        public MigrationProperties(Boolean enabled, String location, String table) {
            if (enabled == null || enabled) {
                this.enabled = true;
                this.location = (location == null || location.isBlank()) ? DEFAULT_LOCATION : location;
                this.table = (table == null || table.isBlank()) ? DEFAULT_TABLE : table;
            } else {
                this.enabled = false;
                this.location = null;
                this.table = null;
            }
        }

        public Boolean isEnabled() {
            return enabled;
        }

        public String getLocation() {
            return location;
        }

        public String getTable() {
            return table;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MigrationProperties that = (MigrationProperties) o;
            return Objects.equals(enabled, that.enabled) &&
                    Objects.equals(location, that.location) &&
                    Objects.equals(table, that.table);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enabled, location, table);
        }
    }
}