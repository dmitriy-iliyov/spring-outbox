package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.starter.consumer.OutboxConsumerProperties;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxPublisherProperties;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.SenderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
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

    @Test
    @DisplayName("UT afterPropertiesSet() when threadPoolSize null should default to available processors capped at 5")
    void afterPropertiesSet_whenThreadPoolSizeNull_shouldDefaultToProcessorCappedAt5() {
        OutboxProperties props = new OutboxProperties();
        props.setThreadPoolSize(null);

        props.afterPropertiesSet();

        int expected = Math.min(Runtime.getRuntime().availableProcessors(), 5);
        assertThat(props.getThreadPoolSize()).isEqualTo(expected);
    }

    @Test
    @DisplayName("UT MetricsProperties afterPropertiesSet() when enabled true should enable gauge with defaults")
    void metrics_afterPropertiesSet_whenEnabledTrue_shouldEnableGaugeWithDefaults() {
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        metrics.setEnabled(true);
        metrics.setGauge(null);

        metrics.afterPropertiesSet();

        assertThat(metrics.isEnabled()).isTrue();
        assertThat(metrics.getGauge()).isNotNull();
        assertThat(metrics.getGauge().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT MetricsProperties afterPropertiesSet() when enabled false should disable gauge")
    void metrics_afterPropertiesSet_whenEnabledFalse_shouldDisableGauge() {
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        metrics.setEnabled(false);

        metrics.afterPropertiesSet();

        assertThat(metrics.isEnabled()).isFalse();
        assertThat(metrics.getGauge()).isNotNull();
        assertThat(metrics.getGauge().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT MetricsProperties afterPropertiesSet() when enabled null should disable metrics")
    void metrics_afterPropertiesSet_whenEnabledNull_shouldDisable() {
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        metrics.setEnabled(null);

        metrics.afterPropertiesSet();

        assertThat(metrics.isEnabled()).isFalse();
        assertThat(metrics.getGauge().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT MetricsProperties afterPropertiesSet() when enabled true and gauge provided should preserve gauge")
    void metrics_afterPropertiesSet_whenEnabledTrueAndGaugeProvided_shouldPreserveGauge() {
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        metrics.setEnabled(true);
        OutboxProperties.MetricsProperties.GaugeProperties gauge = new OutboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        metrics.setGauge(gauge);

        metrics.afterPropertiesSet();

        assertThat(metrics.isEnabled()).isTrue();
        assertThat(metrics.getGauge()).isSameAs(gauge);
        assertThat(metrics.getGauge().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("UT GaugeProperties afterPropertiesSet() when enabled true and cache null should init cache with defaults")
    void gauge_afterPropertiesSet_whenEnabledTrueAndCacheNull_shouldInitCacheDefaults() {
        OutboxProperties.MetricsProperties.GaugeProperties gauge = new OutboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        gauge.setCache(null);

        gauge.afterPropertiesSet();

        assertThat(gauge.isEnabled()).isTrue();
        assertThat(gauge.getCache()).isNotNull();
        assertThat(gauge.getCache().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("UT GaugeProperties afterPropertiesSet() when enabled false should disable cache")
    void gauge_afterPropertiesSet_whenEnabledFalse_shouldDisableCache() {
        OutboxProperties.MetricsProperties.GaugeProperties gauge = new OutboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(false);

        gauge.afterPropertiesSet();

        assertThat(gauge.isEnabled()).isFalse();
        assertThat(gauge.getCache()).isNotNull();
        assertThat(gauge.getCache().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT GaugeProperties afterPropertiesSet() when enabled null should disable gauge and cache")
    void gauge_afterPropertiesSet_whenEnabledNull_shouldDisable() {
        OutboxProperties.MetricsProperties.GaugeProperties gauge = new OutboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(null);

        gauge.afterPropertiesSet();

        assertThat(gauge.isEnabled()).isFalse();
        assertThat(gauge.getCache().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT GaugeProperties afterPropertiesSet() when enabled true and cache provided should preserve cache")
    void gauge_afterPropertiesSet_whenEnabledTrueAndCacheProvided_shouldPreserveCache() {
        OutboxProperties.MetricsProperties.GaugeProperties gauge = new OutboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties cache =
                new OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties();
        cache.setEnabled(true);
        gauge.setCache(cache);

        gauge.afterPropertiesSet();

        assertThat(gauge.getCache()).isSameAs(cache);
        assertThat(gauge.getCache().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("UT CacheProperties afterPropertiesSet() when enabled true and ttls null should use default ttls")
    void cache_afterPropertiesSet_whenEnabledTrueAndTtlsNull_shouldUseDefaults() {
        OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties cache =
                new OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties();
        cache.setEnabled(true);
        cache.setTtls(null);

        cache.afterPropertiesSet();

        assertThat(cache.isEnabled()).isTrue();
        assertThat(cache.getTtls()).hasSize(3);
        assertThat(cache.getTtls()).containsOnly(Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("UT CacheProperties afterPropertiesSet() when enabled true and ttls empty should use default ttls")
    void cache_afterPropertiesSet_whenEnabledTrueAndTtlsEmpty_shouldUseDefaults() {
        OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties cache =
                new OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties();
        cache.setEnabled(true);
        cache.setTtls(List.of());

        cache.afterPropertiesSet();

        assertThat(cache.getTtls()).hasSize(3);
    }

    @Test
    @DisplayName("UT CacheProperties afterPropertiesSet() when enabled true and ttls wrong size should use default ttls")
    void cache_afterPropertiesSet_whenEnabledTrueAndTtlsWrongSize_shouldUseDefaults() {
        OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties cache =
                new OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties();
        cache.setEnabled(true);
        cache.setTtls(List.of(Duration.ofSeconds(10)));

        cache.afterPropertiesSet();

        assertThat(cache.getTtls()).hasSize(3);
        assertThat(cache.getTtls()).containsOnly(Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("UT CacheProperties afterPropertiesSet() when enabled true and ttls correct size should preserve ttls")
    void cache_afterPropertiesSet_whenEnabledTrueAndTtlsCorrectSize_shouldPreserveTtls() {
        OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties cache =
                new OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties();
        cache.setEnabled(true);
        List<Duration> custom = List.of(Duration.ofSeconds(10), Duration.ofSeconds(20), Duration.ofSeconds(30));
        cache.setTtls(custom);

        cache.afterPropertiesSet();

        assertThat(cache.getTtls()).isEqualTo(custom);
    }

    @Test
    @DisplayName("UT CacheProperties afterPropertiesSet() when enabled false should return empty ttls")
    void cache_afterPropertiesSet_whenEnabledFalse_shouldReturnEmptyTtls() {
        OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties cache =
                new OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties();
        cache.setEnabled(false);
        cache.setTtls(List.of(Duration.ofSeconds(10), Duration.ofSeconds(20), Duration.ofSeconds(30)));

        cache.afterPropertiesSet();

        assertThat(cache.isEnabled()).isFalse();
        assertThat(cache.getTtls()).isEmpty();
    }

    @Test
    @DisplayName("UT CacheProperties afterPropertiesSet() when enabled null should enable and use default ttls")
    void cache_afterPropertiesSet_whenEnabledNull_shouldEnableAndUseDefaults() {
        OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties cache =
                new OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties();
        cache.setEnabled(null);

        cache.afterPropertiesSet();

        assertThat(cache.isEnabled()).isTrue();
        assertThat(cache.getTtls()).hasSize(3);
    }

    @Test
    @DisplayName("UT CleanUpProperties afterPropertiesSet() when custom valid values provided should preserve them")
    void cleanUp_afterPropertiesSet_whenCustomValuesProvided_shouldPreserve() {
        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        cleanUp.setEnabled(true);
        cleanUp.setBatchSize(50);
        cleanUp.setTtl(Duration.ofMinutes(30));
        cleanUp.setInitialDelay(Duration.ofSeconds(10));
        cleanUp.setFixedDelay(Duration.ofSeconds(3));

        cleanUp.afterPropertiesSet();

        assertThat(cleanUp.getBatchSize()).isEqualTo(50);
        assertThat(cleanUp.getTtl()).isEqualTo(Duration.ofMinutes(30));
        assertThat(cleanUp.getInitialDelay()).isEqualTo(Duration.ofSeconds(10));
        assertThat(cleanUp.getFixedDelay()).isEqualTo(Duration.ofSeconds(3));
    }
}
