package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.OutboxPropertiesHolder;
import io.github.dmitriyiliyov.springoutbox.starter.consumer.OutboxConsumerProperties;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxPublisherProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@ConfigurationProperties(prefix = "outbox")
public class OutboxProperties implements OutboxPropertiesHolder {

    private static final int DEFAULT_THREAD_POOL_SIZE = Math.min(Runtime.getRuntime().availableProcessors(), 5);
    private static final Logger log = LoggerFactory.getLogger(OutboxProperties.class);

    private Integer threadPoolSize;
    @NestedConfigurationProperty
    private OutboxPublisherProperties publisher;
    @NestedConfigurationProperty
    private OutboxConsumerProperties consumer;
    @NestedConfigurationProperty
    private OutboxProperties.TablesProperties tables;

    public OutboxProperties() {}

    @PostConstruct
    public void afterPropertiesSet() {
        threadPoolSize = threadPoolSize == null ? DEFAULT_THREAD_POOL_SIZE : threadPoolSize;
        if (publisher != null) {
            publisher.afterPropertiesSet();
        } else {
            publisher = new OutboxPublisherProperties();
            publisher.setEnabled(false);
        }
        if (consumer == null) {
            consumer = new OutboxConsumerProperties();
            consumer.setEnabled(false);
        }
        consumer.afterPropertiesSet();
        if (tables == null) {
            tables = new TablesProperties();
            tables.setAutoCreate(true);
        }
        tables.afterPropertiesSet();
        log.debug(this.toString());
    }

    public Integer getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(Integer threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public OutboxPublisherProperties getPublisher() {
        return publisher;
    }

    public void setPublisher(OutboxPublisherProperties publisher) {
        this.publisher = publisher;
    }

    public OutboxConsumerProperties getConsumer() {
        return consumer;
    }

    public void setConsumer(OutboxConsumerProperties consumer) {
        this.consumer = consumer;
    }

    public TablesProperties getTables() {
        return tables;
    }

    public void setTables(TablesProperties tables) {
        this.tables = tables;
    }

    @Override
    public String toString() {
        return "OutboxProperties{" +
                "\n\t threadPoolSize=" + threadPoolSize +
                ",\n\t publisher=" + publisher +
                ",\n\t consumer=" + consumer +
                ",\n\t tables=" + tables +
                '}';
    }

    public static final class CleanUpProperties implements CleanUpPropertiesHolder {

        private static final int DEFAULT_BATCH_SIZE = 100;
        private static final Duration DEFAULT_TTL = Duration.ofHours(1);
        private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofSeconds(120);
        private static final Duration DEFAULT_FIXED_DELAY = Duration.ofSeconds(5);

        private Boolean enabled;
        private Integer batchSize;
        private Duration ttl;
        private Duration initialDelay;
        private Duration fixedDelay;

        public void afterPropertiesSet() {
            if (enabled == null || enabled) {
                enabled = true;
                batchSize = (batchSize == null || batchSize <= 0) ? DEFAULT_BATCH_SIZE : batchSize;
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

        @Override
        public Integer getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
        }

        @Override
        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }

        @Override
        public Duration getInitialDelay() {
            return initialDelay;
        }

        public void setInitialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
        }

        @Override
        public Duration getFixedDelay() {
            return fixedDelay;
        }

        public void setFixedDelay(Duration fixedDelay) {
            this.fixedDelay = fixedDelay;
        }

        @Override
        public String toString() {
            return "CleanUpProperties{" +
                    "enabled=" + enabled +
                    ", batchSize=" + batchSize +
                    ", ttl=" + ttl.toSeconds() +
                    ", initialDelay=" + initialDelay +
                    ", fixedDelay=" + fixedDelay +
                    '}';
        }
    }

    public static final class TablesProperties {

        private Boolean autoCreate;

        public void afterPropertiesSet() {
            autoCreate = autoCreate == null || autoCreate;
        }

        public Boolean isAutoCreate() {
            return autoCreate;
        }

        public void setAutoCreate(Boolean autoCreate) {
            this.autoCreate = autoCreate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TablesProperties that = (TablesProperties) o;
            return Objects.equals(autoCreate, that.autoCreate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(autoCreate);
        }

        @Override
        public String toString() {
            return "TablesProperties{" +
                    "autoCreate=" + autoCreate +
                    '}';
        }
    }

    public static final class MetricsProperties {

        private Boolean enabled;
        @NestedConfigurationProperty
        private GaugeProperties gauge;

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public GaugeProperties getGauge() {
            return gauge;
        }

        public void setGauge(GaugeProperties gauge) {
            this.gauge = gauge;
        }

        public void afterPropertiesSet() {
            if (enabled == null && gauge == null) {
                enabled = false;
            }
            if (enabled == null) {
                enabled = true;
            }
            if (gauge == null) {
                gauge = new GaugeProperties();
                gauge.setEnabled(false);
            }
            gauge.afterPropertiesSet();
        }

        @Override
        public String toString() {
            return "MetricsProperties{" +
                    "enabled=" + enabled +
                    "gauge=" + gauge +
                    '}';
        }

        public static final class GaugeProperties {

            private Boolean enabled;
            @NestedConfigurationProperty
            private CacheProperties cache;

            public Boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(Boolean enabled) {
                this.enabled = enabled;
            }

            public CacheProperties getCache() {
                return cache;
            }

            public void setCache(CacheProperties cache) {
                this.cache = cache;
            }

            public void afterPropertiesSet() {
                if (enabled != null && enabled) {
                    enabled = true;
                } else {
                    enabled = false;
                }
                if (cache == null) {
                    cache = new CacheProperties();
                    cache.setEnabled(false);
                }
                cache.afterPropertiesSet();
            }

            @Override
            public String toString() {
                return "GaugeProperties{" +
                        "enabled=" + enabled +
                        ", cache=" + cache +
                        '}';
            }

            public static final class CacheProperties {

                private static final List<Duration> DEFAULT_TTLS = List.of(
                        Duration.ofSeconds(60), Duration.ofSeconds(60), Duration.ofSeconds(60)
                );

                private Boolean enabled;
                private List<Duration> ttls;

                public Boolean isEnabled() {
                    return enabled;
                }

                public void setEnabled(Boolean enabled) {
                    this.enabled = enabled;
                }

                public List<Duration> getTtls() {
                    return ttls;
                }

                public void setTtls(List<Duration> ttls) {
                    this.ttls = ttls;
                }

                public void afterPropertiesSet() {
                    if (enabled == null || enabled) {
                        enabled = true;
                        if (ttls == null || ttls.isEmpty() || ttls.size() != DEFAULT_TTLS.size()) {
                            ttls = DEFAULT_TTLS;
                        }
                    } else {
                        enabled = false;
                        ttls = Collections.emptyList();
                    }
                }

                @Override
                public String toString() {
                    return "CacheProperties{" +
                            "enabled=" + enabled +
                            ", ttls=" + ttls +
                            '}';
                }
            }
        }
    }
}
