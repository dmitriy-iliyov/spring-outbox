package io.github.dmitriyiliyov.springoutbox.unit.config;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;
import io.github.dmitriyiliyov.springoutbox.consumer.config.OutboxConsumerProperties;
import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherProperties;
import io.github.dmitriyiliyov.springoutbox.publisher.config.SenderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxPropertiesUnitTests {

    @Test
    @DisplayName("UT afterPropertiesSet() should initialize defaults when all nested props are null")
    void afterPropertiesSet_whenAllNestedNull_shouldInitializeDefaults() {
        // given
        OutboxProperties props = new OutboxProperties();
        props.setThreadPoolSize(null);
        props.setPublisher(null);
        props.setConsumer(null);
        props.setTables(null);

        // when
        props.afterPropertiesSet();

        // then
        assertThat(props.getThreadPoolSize()).isNotNull();
        assertThat(props.getPublisher()).isNotNull();
        assertThat(props.getPublisher().isEnabled()).isFalse();
        assertThat(props.getConsumer()).isNotNull();
        assertThat(props.getConsumer().isEnabled()).isFalse();
        assertThat(props.getTables()).isNotNull();
        assertThat(props.getTables().isAutoCreate()).isTrue();
    }

    @Test
    @DisplayName("UT afterPropertiesSet() should preserve threadPoolSize if already set")
    void afterPropertiesSet_whenThreadPoolSizeSet_shouldPreserveValue() {
        // given
        OutboxProperties props = new OutboxProperties();
        props.setThreadPoolSize(10);

        // when
        props.afterPropertiesSet();

        // then
        assertThat(props.getThreadPoolSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("UT afterPropertiesSet() when publisher provided should call publisher.afterPropertiesSet")
    void afterPropertiesSet_whenPublisherProvided_shouldPreserveAndInit() {
        // given
        OutboxProperties props = new OutboxProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties publisher = new OutboxPublisherProperties();
        publisher.setEnabled(true);
        publisher.setSender(sender);
        publisher.setEvents(Map.of("test-event", event));
        props.setPublisher(publisher);

        // when
        props.afterPropertiesSet();

        // then
        assertThat(props.getPublisher()).isNotNull();
        assertThat(props.getPublisher().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("UT afterPropertiesSet() when publisher not provided should set enabled = false")
    void afterPropertiesSet_whenPublisherNotProvided_shouldUnable() {
        // given
        OutboxProperties props = new OutboxProperties();
        props.setPublisher(null);

        // when
        props.afterPropertiesSet();

        // then
        assertThat(props.getPublisher()).isNotNull();
        assertThat(props.getPublisher().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT afterPropertiesSet() when consumer provided and enabled true should initialize nested")
    void afterPropertiesSet_whenConsumerProvidedEnabledTrue_shouldInitNested() {
        // given
        OutboxProperties props = new OutboxProperties();
        String cacheName = "cache";
        OutboxConsumerProperties.CacheProperties cache = new OutboxConsumerProperties.CacheProperties();
        cache.setCacheName(cacheName);
        OutboxConsumerProperties consumer = new OutboxConsumerProperties();
        consumer.setEnabled(true);
        consumer.setCache(cache);
        props.setConsumer(consumer);

        // when
        props.afterPropertiesSet();

        // then
        assertThat(props.getConsumer().isEnabled()).isTrue();
        assertThat(props.getConsumer().getCleanUp()).isNotNull();
        assertThat(props.getConsumer().getCache()).isNotNull();
    }

    @Test
    @DisplayName("UT afterPropertiesSet() when consumer not provided should set enabled=false for consumer")
    void afterPropertiesSet_whenConsumerNotProvided_shouldUnable() {
        // given
        OutboxProperties props = new OutboxProperties();
        props.setConsumer(null);

        // when
        props.afterPropertiesSet();

        // then
        assertThat(props.getConsumer().isEnabled()).isFalse();
        assertThat(props.getConsumer().getCleanUp()).isNull();
        assertThat(props.getConsumer().getCache()).isNull();
    }

    @Test
    @DisplayName("UT afterPropertiesSet() when tables provided should preserve instance")
    void afterPropertiesSet_whenTablesProvided_shouldPreserve() {
        // given
        OutboxProperties props = new OutboxProperties();
        OutboxProperties.TablesProperties tables = new OutboxProperties.TablesProperties();
        tables.setAutoCreate(false);
        props.setTables(tables);

        // when
        props.afterPropertiesSet();

        // then
        assertThat(props.getTables()).isSameAs(tables);
        assertThat(props.getTables().isAutoCreate()).isFalse();
    }

    @Test
    @DisplayName("UT afterPropertiesSet() when tables not provided should set autoCreate = true")
    void afterPropertiesSet_whenTablesNotProvided_shouldSetAutoCreateTrue() {
        // given
        OutboxProperties props = new OutboxProperties();
        props.setTables(null);

        // when
        props.afterPropertiesSet();

        // then
        assertThat(props.getTables()).isNotNull();
        assertThat(props.getTables().isAutoCreate()).isTrue();
    }

    @Test
    @DisplayName("UT CleanUpProperties afterPropertiesSet() should initialize defaults when enabled true or null")
    void cleanUp_afterPropertiesSet_whenEnabledNullOrTrue_shouldInitDefaults() {
        // given
        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        cleanUp.setEnabled(true);
        cleanUp.setBatchSize(null);
        cleanUp.setTtl(null);
        cleanUp.setInitialDelay(null);
        cleanUp.setFixedDelay(null);

        // when
        cleanUp.afterPropertiesSet();

        // then
        assertThat(cleanUp.isEnabled()).isTrue();
        assertThat(cleanUp.getBatchSize()).isEqualTo(100);
        assertThat(cleanUp.getTtl()).isEqualTo(Duration.ofHours(1));
        assertThat(cleanUp.getInitialDelay()).isEqualTo(Duration.ofSeconds(120));
        assertThat(cleanUp.getFixedDelay()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("UT CleanUpProperties afterPropertiesSet() when enabled false should reset values")
    void cleanUp_afterPropertiesSet_whenDisabled_shouldResetValues() {
        // given
        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        cleanUp.setEnabled(false);
        cleanUp.setBatchSize(50);
        cleanUp.setTtl(Duration.ofMinutes(5));
        cleanUp.setInitialDelay(Duration.ofSeconds(1));
        cleanUp.setFixedDelay(Duration.ofSeconds(1));

        // when
        cleanUp.afterPropertiesSet();

        // then
        assertThat(cleanUp.isEnabled()).isFalse();
        assertThat(cleanUp.getBatchSize()).isZero();
        assertThat(cleanUp.getTtl()).isNull();
        assertThat(cleanUp.getInitialDelay()).isNull();
        assertThat(cleanUp.getFixedDelay()).isNull();
    }

    @Test
    @DisplayName("UT CleanUpProperties afterPropertiesSet() when enabled null should default enabled true")
    void cleanUp_afterPropertiesSet_whenEnabledNull_shouldEnable() {
        // given
        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        cleanUp.setEnabled(null);

        // when
        cleanUp.afterPropertiesSet();

        // then
        assertThat(cleanUp.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("UT CleanUpProperties afterPropertiesSet() when batchSize negative should reset to default")
    void cleanUp_afterPropertiesSet_whenBatchSizeNegative_shouldReset() {
        // given
        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        cleanUp.setEnabled(true);
        cleanUp.setBatchSize(-10);

        // when
        cleanUp.afterPropertiesSet();

        // then
        assertThat(cleanUp.getBatchSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("UT TablesProperties afterPropertiesSet() when autoCreate null should default true")
    void tables_afterPropertiesSet_whenAutoCreateNull_shouldDefaultTrue() {
        // given
        OutboxProperties.TablesProperties tables = new OutboxProperties.TablesProperties();
        tables.setAutoCreate(null);

        // when
        tables.afterPropertiesSet();

        // then
        assertThat(tables.isAutoCreate()).isTrue();
    }

    @Test
    @DisplayName("UT TablesProperties afterPropertiesSet() when autoCreate false should preserve false")
    void tables_afterPropertiesSet_whenAutoCreateFalse_shouldPreserve() {
        // given
        OutboxProperties.TablesProperties tables = new OutboxProperties.TablesProperties();
        tables.setAutoCreate(false);

        // when
        tables.afterPropertiesSet();

        // then
        assertThat(tables.isAutoCreate()).isFalse();
    }

    @Test
    @DisplayName("UT CleanUpProperties afterPropertiesSet() when batchSize <= 0 should reset to default")
    void cleanUp_afterPropertiesSet_whenBatchSizeZero_shouldDefaultTo100() {
        // given
        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        cleanUp.setEnabled(true);
        cleanUp.setBatchSize(0);

        // when
        cleanUp.afterPropertiesSet();

        // then
        assertThat(cleanUp.getBatchSize()).isEqualTo(100);
    }
}
