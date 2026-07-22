package io.github.dmitriyiliyov.oncebox.starter.publisher.dlq;

import io.github.dmitriyiliyov.oncebox.metrics.publisher.utils.NoopOutboxCache;
import io.github.dmitriyiliyov.oncebox.metrics.publisher.utils.OutboxCache;
import io.github.dmitriyiliyov.oncebox.metrics.publisher.utils.SimpleOutboxCache;
import io.github.dmitriyiliyov.oncebox.starter.OutboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.publisher.OutboxPublisherProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxDlqMetricsAutoConfigurationUnitTests {

    @Test
    @DisplayName("UT outboxDlqCache returns noop cache when metrics null")
    void outboxDlqCache_metricsNull_returnsPassthrough() {
        // given
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        props.setDlq(new OutboxPublisherProperties.DlqProperties());
        OutboxDlqMetricsAutoConfiguration config = new OutboxDlqMetricsAutoConfiguration(props);

        // when
        OutboxCache<?> cache = config.outboxDlqCache();

        // then
        assertThat(cache).isInstanceOf(NoopOutboxCache.class).isNotNull();
    }

    @Test
    @DisplayName("UT outboxDlqCache returns noop cache when gaugeProperties null")
    void outboxDlqCache_gaugeNull_returnsPassthrough() {
        // given
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        metrics.setEnabled(true);
        metrics.setGauge(null);
        props.setMetrics(metrics);
        props.setDlq(dlq);

        OutboxDlqMetricsAutoConfiguration config = new OutboxDlqMetricsAutoConfiguration(props);

        // when
        OutboxCache<?> cache = config.outboxDlqCache();

        // then
        assertThat(cache).isInstanceOf(NoopOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxDlqCache returns Passthrough cache when gauge disabled")
    void outboxDlqCache_gaugeDisabled_returnsPassthrough() {
        // given
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        metrics.setEnabled(true);
        OutboxProperties.MetricsProperties.GaugeProperties gauge = new OutboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(false);
        metrics.setGauge(gauge);
        props.setMetrics(metrics);
        props.setDlq(dlq);

        OutboxDlqMetricsAutoConfiguration config = new OutboxDlqMetricsAutoConfiguration(props);

        // when
        OutboxCache<?> cache = config.outboxDlqCache();

        // then
        assertThat(cache).isInstanceOf(NoopOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxDlqCache returns SimpleOutboxCache when ttls size == 3 and enabled")
    void outboxDlqCache_validTtls_returnsSimpleCache() {
        // given
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        metrics.setEnabled(true);
        OutboxProperties.MetricsProperties.GaugeProperties gauge = new OutboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);

        OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties cacheProps =
                new OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties();
        cacheProps.setEnabled(true);
        cacheProps.setTtls(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)));
        gauge.setCache(cacheProps);
        metrics.setGauge(gauge);
        props.setMetrics(metrics);
        props.setDlq(dlq);

        OutboxDlqMetricsAutoConfiguration config = new OutboxDlqMetricsAutoConfiguration(props);

        // when
        OutboxCache<?> cache = config.outboxDlqCache();

        // then
        assertThat(cache).isInstanceOf(SimpleOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxDlqCache returns noop cache when gauge enabled but cache disabled")
    void outboxDlqCache_gaugeEnabledCacheDisabled_returnsPassthrough() {
        // given
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        props.setDlq(new OutboxPublisherProperties.DlqProperties());
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        metrics.setEnabled(true);
        OutboxProperties.MetricsProperties.GaugeProperties gauge = new OutboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties cacheProps =
                new OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties();
        cacheProps.setEnabled(false);
        gauge.setCache(cacheProps);
        metrics.setGauge(gauge);
        // applyDefaults empties the ttls of a disabled cache
        metrics.applyDefaults();
        props.setMetrics(metrics);

        OutboxDlqMetricsAutoConfiguration config = new OutboxDlqMetricsAutoConfiguration(props);

        // when
        OutboxCache<?> cache = config.outboxDlqCache();

        // then
        assertThat(cache).isInstanceOf(NoopOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxDlqCache returns noop cache when gauge enabled flag is null")
    void outboxDlqCache_gaugeEnabledNull_returnsPassthrough() {
        // given
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        props.setDlq(new OutboxPublisherProperties.DlqProperties());
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        OutboxProperties.MetricsProperties.GaugeProperties gauge = new OutboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(null);
        metrics.setGauge(gauge);
        props.setMetrics(metrics);

        OutboxDlqMetricsAutoConfiguration config = new OutboxDlqMetricsAutoConfiguration(props);

        // when
        OutboxCache<?> cache = config.outboxDlqCache();

        // then
        assertThat(cache).isInstanceOf(NoopOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxDlqCache returns SimpleOutboxCache when cache enabled flag is null")
    void outboxDlqCache_cacheEnabledNull_returnsSimpleCache() {
        // given
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        props.setDlq(new OutboxPublisherProperties.DlqProperties());
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        metrics.setEnabled(true);
        OutboxProperties.MetricsProperties.GaugeProperties gauge = new OutboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties cacheProps =
                new OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties();
        cacheProps.setEnabled(null);
        cacheProps.setTtls(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)));
        gauge.setCache(cacheProps);
        metrics.setGauge(gauge);
        props.setMetrics(metrics);

        OutboxDlqMetricsAutoConfiguration config = new OutboxDlqMetricsAutoConfiguration(props);

        // when
        OutboxCache<?> cache = config.outboxDlqCache();

        // then
        assertThat(cache).isInstanceOf(SimpleOutboxCache.class);
    }
}
