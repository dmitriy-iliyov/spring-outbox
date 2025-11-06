package io.github.dmitriyiliyov.springoutbox.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "outbox")
public class OutboxProperties {

    private static final int DEFAULT_THREAD_POOL_SIZE = Math.min(Runtime.getRuntime().availableProcessors(), 5);
    private static final Logger log = LoggerFactory.getLogger(OutboxProperties.class);

    private SenderProperties sender;
    private Integer threadPoolSize;
    private Defaults defaults;
    private Map<String, EventProperties> events;
    private StuckRecoveryProperties stuckRecovery;
    private CleanUpProperties cleanUp;
    private DlqProperties dlq;

    public OutboxProperties() {}

    @PostConstruct
    public void initialize() {
        if (sender == null) {
            throw new IllegalArgumentException("sender cannot be null");
        }
        sender.initialize();
        threadPoolSize = threadPoolSize == null ? DEFAULT_THREAD_POOL_SIZE : threadPoolSize;
        defaults = defaults == null ? new Defaults() : defaults;
        defaults.initialize();
        if (events == null) {
            throw new IllegalArgumentException("events cannot be null");
        }
        if (events.isEmpty()) {
            log.warn("Outbox is configured without events");
        }
        events = applyDefaults(events);
        stuckRecovery = stuckRecovery == null ?
                new StuckRecoveryProperties() : stuckRecovery;
        stuckRecovery.initialize();
        if (cleanUp != null) {
            cleanUp.initialize();
        } else {
            log.warn("Outbox is configured with disabled clean-up, outbox storage will not be cleaned automatically.");
        }
        if (dlq != null) {
            dlq.initialize();
        } else {
            log.warn("Outbox is configured with disabled dlq, failed outbox events will not be managed automatically.");
        }
        log.debug("OutboxProperties successfully initialized");
    }

    private Map<String, EventProperties> applyDefaults(Map<String, EventProperties> eventPropertiesMap) {
        return eventPropertiesMap.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        e -> {
                            String eventType = e.getKey();
                            if (eventType == null) {
                                throw new IllegalArgumentException("eventType cannot be null");
                            }
                            if (eventType.isBlank()) {
                                throw new IllegalArgumentException("eventType cannot be blank");
                            }
                            return eventType;
                        },
                        e -> {
                            EventProperties event = e.getValue();
                            event.setEventType(e.getKey());
                            event.initialize(defaults);
                            return event;
                        }
                ));
    }

    public SenderProperties getSender() {
        return sender;
    }

    public void setSender(SenderProperties sender) {
        this.sender = sender;
    }

    public Integer getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(Integer threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public OutboxProperties.Defaults getDefaults() {
        return this.defaults;
    }

    public void setDefaults(Defaults defaults) {
        this.defaults = defaults;
    }

    public boolean existEventType(String eventType) {
        return events.containsKey(eventType);
    }

    public Map<String, EventProperties> getEvents() {
        return events;
    }

    public void setEvents(Map<String, EventProperties> events) {
        this.events = events;
    }

    public StuckRecoveryProperties getStuckRecovery() {
        return stuckRecovery;
    }

    public void setStuckRecovery(StuckRecoveryProperties stuckRecovery) {
        this.stuckRecovery = stuckRecovery;
    }

    public boolean isCleanUpEnabled() {
        if (cleanUp == null) {
            return false;
        }
        return cleanUp.isEnabled();
    }

    public CleanUpProperties getCleanUp() {
        return cleanUp;
    }

    public void setCleanUp(CleanUpProperties cleanUp) {
        this.cleanUp = cleanUp;
    }

    public DlqProperties getDlq() {
        return dlq;
    }

    public void setDlq(DlqProperties dlq) {
        this.dlq = dlq;
    }

    public static final class SenderProperties {

        private SenderType type;
        private String beanName;

        public void initialize() {
            if (type == null) {
                throw new IllegalArgumentException("senderType cannot be null");
            }
        }

        public SenderType getType() {
            return type;
        }

        public void setType(SenderType type) {
            this.type = type;
        }

        public String getBeanName() {
            return beanName;
        }

        public void setBeanName(String beanName) {
            this.beanName = beanName;
        }
    }

    public static final class Defaults {

        private static final int DEFAULT_BATCH_SIZE = 50;
        private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofSeconds(300);
        private static final Duration DEFAULT_FIXED_DELAY = Duration.ofSeconds(2);
        private static final int DEFAULT_MAX_RETRY = 3;
        private static final BackoffProperties DEFAULT_BACKOFF = new BackoffProperties();

        private Integer batchSize;
        private Duration initialDelay;
        private Duration fixedDelay;
        private Integer maxRetries;
        private BackoffProperties backoff;

        public Defaults() {
            this.batchSize = DEFAULT_BATCH_SIZE;
            this.initialDelay = DEFAULT_INITIAL_DELAY;
            this.fixedDelay = DEFAULT_FIXED_DELAY;
            this.maxRetries = DEFAULT_MAX_RETRY;
            this.backoff = DEFAULT_BACKOFF;
        }

        public void initialize() {
            batchSize = batchSize == null || batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
            initialDelay = initialDelay == null ? DEFAULT_INITIAL_DELAY : initialDelay;
            fixedDelay = fixedDelay == null ? DEFAULT_FIXED_DELAY : fixedDelay;
            maxRetries = maxRetries == null || maxRetries < 0 ? DEFAULT_MAX_RETRY : maxRetries;
            backoff = backoff == null ? DEFAULT_BACKOFF : backoff;
            backoff.initialize();
        }

        public void setBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setInitialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
        }

        public Duration getInitialDelay() {
            return initialDelay;
        }

        public void setFixedDelay(Duration fixedDelay) {
            this.fixedDelay = fixedDelay;
        }

        public Duration getFixedDelay() {
            return fixedDelay;
        }

        public void setMaxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setBackoff(BackoffProperties backoff) {
            this.backoff = backoff;
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

        private Boolean enabled;
        private Duration delay;
        private Long multiplier;

        public BackoffProperties() {
            this.enabled = true;
            this.delay = DEFAULT_DELAY;
            this.multiplier = DEFAULT_MULTIPLIER;
        }

        public BackoffProperties(Boolean enabled, Duration delay, Long multiplier) {
            this.enabled = enabled;
            this.delay = delay;
            this.multiplier = multiplier;
            this.initialize();
        }

        public void initialize() {
            if (enabled == null || enabled) {
                enabled = true;
                delay = delay == null ? DEFAULT_DELAY : delay;
                multiplier = multiplier == null || multiplier < 1 ? DEFAULT_MULTIPLIER : multiplier;
            } else {
                enabled = false;
                delay = Duration.ofSeconds(0);
                multiplier = 1L;
            }
        }

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getDelay() {
            return delay;
        }

        public void setDelay(Duration delay) {
            this.delay = delay;
        }

        public Long getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(Long multiplier) {
            this.multiplier = multiplier;
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

    public static final class EventProperties {

        private String eventType;
        private String topic;
        private Integer batchSize;
        private Duration initialDelay;
        private Duration fixedDelay;
        private Integer maxRetries;
        private BackoffProperties backoff;

        public void initialize(Defaults defaults) {
            if (eventType == null) {
                throw new IllegalArgumentException("eventType cannot be null");
            }
            if (eventType.isBlank()) {
                throw new IllegalArgumentException("eventType cannot be blank");
            }
            if (topic == null) {
                throw new IllegalArgumentException("topic cannot be null");
            }
            if (topic.isBlank()) {
                throw new IllegalArgumentException("topic cannot be blank");
            }
            batchSize = batchSize == null ? defaults.getBatchSize() : batchSize;
            initialDelay = initialDelay == null ? defaults.getInitialDelay() : initialDelay;
            fixedDelay = fixedDelay == null ? defaults.getFixedDelay() : fixedDelay;
            maxRetries = maxRetries == null ? defaults.getMaxRetries() : maxRetries;
            if (backoff == null) {
                backoff = defaults.getBackoff();
            } else if (!backoff.isEnabled()) {
                backoff = new BackoffProperties();
                backoff.setEnabled(false);
            } else {
                backoff = new BackoffProperties(
                        true,
                        backoff.getDelay() == null ?
                                defaults.getBackoff().getDelay() :
                                backoff.getDelay(),
                        backoff.getMultiplier() == null || backoff.getMultiplier() < 1 ?
                                defaults.getBackoff().getMultiplier() :
                                backoff.getMultiplier()
                );
            }
            backoff.initialize();
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public Integer getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
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

        public Integer getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
        }

        public BackoffProperties getBackoff() {
            return backoff;
        }

        public void setBackoff(BackoffProperties backoff) {
            this.backoff = backoff;
        }

        public long backoffMultiplier() {
            return backoff.getMultiplier();
        }

        public long backoffDelay() {
            return backoff.getDelay().toSeconds();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EventProperties that)) return false;
            return Objects.equals(eventType, that.eventType)
                    && Objects.equals(topic, that.topic)
                    && Objects.equals(batchSize, that.batchSize)
                    && Objects.equals(initialDelay, that.initialDelay)
                    && Objects.equals(fixedDelay, that.fixedDelay)
                    && Objects.equals(maxRetries, that.maxRetries)
                    && Objects.equals(backoff, that.backoff);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventType, topic, batchSize, initialDelay, fixedDelay, maxRetries, backoff);
        }
    }

    public static final class StuckRecoveryProperties {

        private static final int DEFAULT_BATCH_SIZE = 100;
        private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofSeconds(300);
        private static final Duration DEFAULT_FIXED_DELAY = Duration.ofSeconds(1800);

        private Integer batchSize;
        private Duration initialDelay;
        private Duration fixedDelay;

        public StuckRecoveryProperties() {
            this.batchSize = DEFAULT_BATCH_SIZE;
            this.initialDelay = DEFAULT_INITIAL_DELAY;
            this.fixedDelay = DEFAULT_FIXED_DELAY;
        }

        public void initialize() {
            batchSize = batchSize == null || batchSize < 0 ? DEFAULT_BATCH_SIZE : batchSize;
            initialDelay = initialDelay == null ? DEFAULT_INITIAL_DELAY : initialDelay;
            fixedDelay = fixedDelay == null ? DEFAULT_FIXED_DELAY : fixedDelay;
        }

        public Integer getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StuckRecoveryProperties that = (StuckRecoveryProperties) o;
            return Objects.equals(batchSize, that.batchSize) &&
                    Objects.equals(initialDelay, that.initialDelay) &&
                    Objects.equals(fixedDelay, that.fixedDelay);
        }

        @Override
        public int hashCode() {
            return Objects.hash(batchSize, initialDelay, fixedDelay);
        }
    }

    public static final class CleanUpProperties {

        private static final int DEFAULT_BATCH_SIZE = 100;
        private static final Duration DEFAULT_THRESHOLD = Duration.ofHours(1);
        private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofSeconds(300);
        private static final Duration DEFAULT_FIXED_DELAY = Duration.ofSeconds(5);

        private Boolean enabled;
        private Integer batchSize;
        private Duration threshold;
        private Duration initialDelay;
        private Duration fixedDelay;

        public void initialize() {
            if (enabled != null && enabled) {
                enabled = true;
                batchSize = (batchSize == null || batchSize < 0) ? DEFAULT_BATCH_SIZE : batchSize;
                threshold = (threshold == null) ? DEFAULT_THRESHOLD : threshold;
                initialDelay = (initialDelay == null) ? DEFAULT_INITIAL_DELAY : initialDelay;
                fixedDelay = (fixedDelay == null) ? DEFAULT_FIXED_DELAY : fixedDelay;
            } else {
                enabled = false;
                batchSize = 0;
                threshold = null;
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

        public Duration getThreshold() {
            return threshold;
        }

        public void setThreshold(Duration threshold) {
            this.threshold = threshold;
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

    public static final class DlqProperties {

        private static final int DEFAULT_BATCH_SIZE = 100;
        private static final Duration DEFAULT_TO_INITIAL_DELAY = Duration.ofSeconds(300);
        private static final Duration DEFAULT_TO_FIXED_DELAY = Duration.ofSeconds(900);
        private static final Duration DEFAULT_FROM_INITIAL_DELAY = Duration.ofSeconds(300);
        private static final Duration DEFAULT_FROM_FIXED_DELAY = Duration.ofSeconds(3600);

        private Boolean enabled;
        private Integer batchSize;
        private Duration transferToInitialDelay;
        private Duration transferToFixedDelay;
        private Duration transferFromInitialDelay;
        private Duration transferFromFixedDelay;

        public void initialize() {
            if (enabled != null && enabled) {
                enabled = true;
                batchSize = batchSize == null || batchSize < 0 ? DEFAULT_BATCH_SIZE : batchSize;
                transferToInitialDelay = transferToInitialDelay == null ? DEFAULT_TO_INITIAL_DELAY : transferToInitialDelay;
                transferToFixedDelay = transferToFixedDelay == null ? DEFAULT_TO_FIXED_DELAY : transferToFixedDelay;
                transferFromInitialDelay = transferFromInitialDelay == null ? DEFAULT_FROM_INITIAL_DELAY : transferFromInitialDelay;
                transferFromFixedDelay = transferFromFixedDelay == null ? DEFAULT_FROM_FIXED_DELAY : transferFromFixedDelay;
            } else {
                enabled = false;
                batchSize = 0;
                transferToInitialDelay = null;
                transferToFixedDelay = null;
                transferFromInitialDelay = null;
                transferFromFixedDelay = null;
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

        public Duration getTransferToInitialDelay() {
            return transferToInitialDelay;
        }

        public void setTransferToInitialDelay(Duration transferToInitialDelay) {
            this.transferToInitialDelay = transferToInitialDelay;
        }

        public Duration getTransferToFixedDelay() {
            return transferToFixedDelay;
        }

        public void setTransferToFixedDelay(Duration transferToFixedDelay) {
            this.transferToFixedDelay = transferToFixedDelay;
        }

        public Duration getTransferFromInitialDelay() {
            return transferFromInitialDelay;
        }

        public void setTransferFromInitialDelay(Duration transferFromInitialDelay) {
            this.transferFromInitialDelay = transferFromInitialDelay;
        }

        public Duration getTransferFromFixedDelay() {
            return transferFromFixedDelay;
        }

        public void setTransferFromFixedDelay(Duration transferFromFixedDelay) {
            this.transferFromFixedDelay = transferFromFixedDelay;
        }
    }
}
