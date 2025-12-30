package io.github.dmitriyiliyov.springoutbox.unit.publisher.config;

import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherAutoConfiguration;
import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherProperties;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.OutboxCache;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.PassthroughOutboxCache;
import io.github.dmitriyiliyov.springoutbox.publisher.utils.SimpleOutboxCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxPublisherAutoConfigurationUnitTests {

    private final OutboxPublisherProperties props = new OutboxPublisherProperties();
    private final OutboxPublisherAutoConfiguration config = new OutboxPublisherAutoConfiguration(props, Mockito.mock(com.fasterxml.jackson.databind.ObjectMapper.class));

    @Test
    @DisplayName("UT outboxCache returns Passthrough when metrics null")
    void outboxCache_metricsNull_returnsPassthrough() {
        props.setMetrics(null);

        OutboxCache<?> cache = config.outboxCache();

        assertThat(cache).isInstanceOf(PassthroughOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxCache returns Passthrough when gauge null")
    void outboxCache_gaugeNull_returnsPassthrough() {
        OutboxPublisherProperties.MetricsProperties metrics = new OutboxPublisherProperties.MetricsProperties();
        metrics.setGauge(null);
        props.setMetrics(metrics);

        OutboxCache<?> cache = config.outboxCache();

        assertThat(cache).isInstanceOf(PassthroughOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxCache returns Passthrough when gauge disabled")
    void outboxCache_gaugeDisabled_returnsPassthrough() {
        OutboxPublisherProperties.MetricsProperties metrics = new OutboxPublisherProperties.MetricsProperties();
        OutboxPublisherProperties.MetricsProperties.GaugeProperties gauge = new OutboxPublisherProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(false);
        metrics.setGauge(gauge);
        props.setMetrics(metrics);

        OutboxCache<?> cache = config.outboxCache();

        assertThat(cache).isInstanceOf(PassthroughOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxCache throws when ttls null or empty")
    void outboxCache_ttlsNullOrEmpty_throws() {
        OutboxPublisherProperties.MetricsProperties metrics = new OutboxPublisherProperties.MetricsProperties();
        OutboxPublisherProperties.MetricsProperties.GaugeProperties gauge = new OutboxPublisherProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        gauge.setCache(new OutboxPublisherProperties.MetricsProperties.GaugeProperties.CacheProperties());
        metrics.setGauge(gauge);
        props.setMetrics(metrics);

        assertThatThrownBy(() -> config.outboxCache())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cache ttls cannot be null or empty");
    }

    @Test
    @DisplayName("UT outboxCache throws when ttls size != 3")
    void outboxCache_ttlsSizeIncorrect_throws() {
        OutboxPublisherProperties.MetricsProperties metrics = new OutboxPublisherProperties.MetricsProperties();
        OutboxPublisherProperties.MetricsProperties.GaugeProperties gauge = new OutboxPublisherProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        OutboxPublisherProperties.MetricsProperties.GaugeProperties.CacheProperties cacheProps =
                new OutboxPublisherProperties.MetricsProperties.GaugeProperties.CacheProperties();
        cacheProps.setTtls(List.of(Duration.ofSeconds(1)));
        gauge.setCache(cacheProps);
        metrics.setGauge(gauge);
        props.setMetrics(metrics);

        assertThatThrownBy(() -> config.outboxCache())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ttls should be 3 element size");
    }

    @Test
    @DisplayName("UT outboxCache returns SimpleOutboxCache when valid ttls")
    void outboxCache_validTtls_returnsSimpleCache() {
        OutboxPublisherProperties.MetricsProperties metrics = new OutboxPublisherProperties.MetricsProperties();
        OutboxPublisherProperties.MetricsProperties.GaugeProperties gauge = new OutboxPublisherProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        OutboxPublisherProperties.MetricsProperties.GaugeProperties.CacheProperties cacheProps =
                new OutboxPublisherProperties.MetricsProperties.GaugeProperties.CacheProperties();
        cacheProps.setTtls(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)));
        gauge.setCache(cacheProps);
        metrics.setGauge(gauge);
        props.setMetrics(metrics);

        OutboxCache<?> cache = config.outboxCache();

        assertThat(cache).isInstanceOf(SimpleOutboxCache.class);
    }
}
