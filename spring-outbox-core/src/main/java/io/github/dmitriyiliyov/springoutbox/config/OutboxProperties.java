package io.github.dmitriyiliyov.springoutbox.config;

import io.github.dmitriyiliyov.springoutbox.consumer.config.OutboxConsumerProperties;
import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Objects;

@ConfigurationProperties(prefix = "outbox")
public class OutboxProperties {

    private static final int DEFAULT_THREAD_POOL_SIZE = Math.min(Runtime.getRuntime().availableProcessors(), 5);

    private Integer threadPoolSize;
    @NestedConfigurationProperty
    private OutboxPublisherProperties publisher;
    @NestedConfigurationProperty
    private OutboxConsumerProperties consumer;
    private OutboxProperties.TablesProperties tables;

    public OutboxProperties() { }

    @PostConstruct
    public void initialize() {
        threadPoolSize = threadPoolSize == null ? DEFAULT_THREAD_POOL_SIZE : threadPoolSize;
        if (publisher != null) {
            publisher.initialize();
        }
        if (consumer != null) {
            consumer.initialize();
        }
        tables = tables == null ? new TablesProperties() : tables;
        tables.initialize();
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

    public static final class TablesProperties {

        private Boolean autoCreate;

        public TablesProperties() {
            this.autoCreate = true;
        }

        public void initialize() {
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
    }
}
