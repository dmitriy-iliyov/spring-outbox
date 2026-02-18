package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.publisher.DefaultOutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxRepository;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.OutboxManagerMetricsDecorator;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.NoopOutboxCache;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.OutboxCache;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.SimpleOutboxCache;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxPublisherAutoConfiguration;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxPublisherProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class OutboxPublisherAutoConfigurationUnitTests {

    private final OutboxPublisherProperties props = new OutboxPublisherProperties();
    private final OutboxPublisherAutoConfiguration config = new OutboxPublisherAutoConfiguration(props, Mockito.mock(com.fasterxml.jackson.databind.ObjectMapper.class));

    @Test
    @DisplayName("UT outboxCache returns noop when metrics null")
    void outboxCache_metricsNull_returnsNoop() {
        props.setMetrics(null);

        OutboxCache<?> cache = config.outboxCache();

        assertThat(cache).isInstanceOf(NoopOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxCache returns noop when gauge null")
    void outboxCache_gaugeNull_returnsNoop() {
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        metrics.setGauge(null);
        props.setMetrics(metrics);

        OutboxCache<?> cache = config.outboxCache();

        assertThat(cache).isInstanceOf(NoopOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxCache returns noop when gauge disabled")
    void outboxCache_gaugeDisabled_returnsNoop() {
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        OutboxProperties.MetricsProperties.GaugeProperties gauge = new OutboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(false);
        metrics.setGauge(gauge);
        props.setMetrics(metrics);

        OutboxCache<?> cache = config.outboxCache();

        assertThat(cache).isInstanceOf(NoopOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxCache throws when ttls null or empty")
    void outboxCache_ttlsNullOrEmpty_throws() {
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        OutboxProperties.MetricsProperties.GaugeProperties gauge = new OutboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        gauge.setCache(new OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties());
        metrics.setGauge(gauge);
        props.setMetrics(metrics);

        assertThatThrownBy(() -> config.outboxCache())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cache ttls cannot be null or empty");
    }

    @Test
    @DisplayName("UT outboxCache throws when ttls size != 3")
    void outboxCache_ttlsSizeIncorrect_throws() {
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        OutboxProperties.MetricsProperties.GaugeProperties gauge = new OutboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties cacheProps =
                new OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties();
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
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        OutboxProperties.MetricsProperties.GaugeProperties gauge = new OutboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties cacheProps =
                new OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties();
        cacheProps.setTtls(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)));
        gauge.setCache(cacheProps);
        metrics.setGauge(gauge);
        props.setMetrics(metrics);

        OutboxCache<?> cache = config.outboxCache();

        assertThat(cache).isInstanceOf(SimpleOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxManager returns default manager when metrics disabled")
    void outboxManager_metricsDisabled_returnsDefault() {
        // given
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        metrics.setEnabled(false);
        props.setMetrics(metrics);

        OutboxRepository repository = Mockito.mock(OutboxRepository.class);
        MeterRegistry registry = Mockito.mock(MeterRegistry.class);

        // when
        OutboxManager manager = config.outboxManager(repository, registry);

        // then
        assertThat(manager).isInstanceOf(DefaultOutboxManager.class);
    }

    @Test
    @DisplayName("UT outboxManager returns default manager when metrics null")
    void outboxManager_metricsNull_returnsDefault() {
        // given
        props.setMetrics(null);

        OutboxRepository repository = Mockito.mock(OutboxRepository.class);
        MeterRegistry registry = Mockito.mock(MeterRegistry.class);

        // when
        OutboxManager manager = config.outboxManager(repository, registry);

        // then
        assertThat(manager).isInstanceOf(DefaultOutboxManager.class);
    }

    @Test
    @DisplayName("UT outboxManager returns metrics decorator when metrics enabled")
    void outboxManager_metricsEnabled_returnsDecorator() {
        // given
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        metrics.setEnabled(true);
        props.setMetrics(metrics);
        props.setEvents(Map.of());

        OutboxRepository repository = Mockito.mock(OutboxRepository.class);
        MeterRegistry registry = Mockito.mock(MeterRegistry.class);
        when(registry.counter(anyString(), anyString(), anyString())).thenReturn(Mockito.mock(Counter.class));

        // when
        OutboxManager manager = config.outboxManager(repository, registry);

        // then
        assertThat(manager).isInstanceOf(OutboxManagerMetricsDecorator.class);
    }
}
