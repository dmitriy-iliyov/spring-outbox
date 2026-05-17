package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.NoopOutboxCache;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.OutboxCache;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.SimpleOutboxCache;
import io.github.dmitriyiliyov.springoutbox.starter.OutboxProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class OutboxPublisherMetricsAutoConfigurationUnitTests {

    private OutboxPublisherProperties props;
    private OutboxPublisherMetricsAutoConfiguration config;

    @Mock
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        props = mock(OutboxPublisherProperties.class);
        config = new OutboxPublisherMetricsAutoConfiguration(props);
    }

    @Test
    @DisplayName("UT outboxCache returns noop when metrics null")
    void outboxCache_metricsNull_returnsNoop() {
        when(props.getMetrics()).thenReturn(null);

        OutboxCache<?> cache = config.outboxCache();

        assertThat(cache).isInstanceOf(NoopOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxCache returns noop when gauge null")
    void outboxCache_gaugeNull_returnsNoop() {
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        metrics.setGauge(null);
        when(props.getMetrics()).thenReturn(metrics);

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
        when(props.getMetrics()).thenReturn(metrics);

        OutboxCache<?> cache = config.outboxCache();

        assertThat(cache).isInstanceOf(NoopOutboxCache.class);
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
        when(props.getMetrics()).thenReturn(metrics);

        OutboxCache<?> cache = config.outboxCache();

        assertThat(cache).isInstanceOf(SimpleOutboxCache.class);
    }
}
