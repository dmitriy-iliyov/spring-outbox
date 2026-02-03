package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqTransfer;
import io.github.dmitriyiliyov.springoutbox.core.publisher.utils.OutboxCache;
import io.github.dmitriyiliyov.springoutbox.core.publisher.utils.PassthroughOutboxCache;
import io.github.dmitriyiliyov.springoutbox.core.publisher.utils.SimpleOutboxCache;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxDlqAutoConfiguration;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxPublisherProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxDlqAutoConfigurationUnitTests {

    private final OutboxDlqAutoConfiguration config = new OutboxDlqAutoConfiguration();

    @Test
    @DisplayName("UT outboxDlqCache returns Passthrough cache when metrics null")
    void outboxDlqCache_metricsNull_returnsPassthrough() {
        // given
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        props.setDlq(new OutboxPublisherProperties.DlqProperties());

        // when
        OutboxCache<?> cache = config.outboxDlqCache(props);

        // then
        assertThat(cache).isInstanceOf(PassthroughOutboxCache.class).isNotNull();
    }

    @Test
    @DisplayName("UT outboxDlqCache throws when ttls null or empty")
    void outboxDlqCache_ttlsNull_throws() {
        // given
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        props.setDlq(dlq);

        OutboxPublisherProperties.MetricsProperties metrics = new OutboxPublisherProperties.MetricsProperties();
        OutboxPublisherProperties.MetricsProperties.GaugeProperties gauge = new OutboxPublisherProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        gauge.setCache(new OutboxPublisherProperties.MetricsProperties.GaugeProperties.CacheProperties());
        metrics.setGauge(gauge);

        dlq.setMetrics(metrics);

        // when + then
        assertThatThrownBy(() -> config.outboxDlqCache(props))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cache ttls cannot be null or empty");
    }

    @Test
    @DisplayName("UT outboxDlqCache throws when ttls size != 3")
    void outboxDlqCache_ttlsSizeIncorrect_throws() {
        // given
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        OutboxPublisherProperties.MetricsProperties metrics = new OutboxPublisherProperties.MetricsProperties();
        OutboxPublisherProperties.MetricsProperties.GaugeProperties gauge = new OutboxPublisherProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        gauge.setCache(new OutboxPublisherProperties.MetricsProperties.GaugeProperties.CacheProperties());
        gauge.getCache().setTtls(List.of(Duration.ofSeconds(1)));
        metrics.setGauge(gauge);
        props.setDlq(new OutboxPublisherProperties.DlqProperties());
        props.getDlq().setMetrics(metrics);

        // when + then
        assertThatThrownBy(() -> config.outboxDlqCache(props))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ttls should be 3 element size");
    }

    @Test
    @DisplayName("UT outboxDlqScheduler throws when DlqProperties null")
    void outboxDlqScheduler_dlqPropertiesNull_throws() {
        // given
        OutboxPublisherProperties props = new OutboxPublisherProperties();

        // when + then
        assertThatThrownBy(() -> config.outboxDlqScheduler(Mockito.mock(ScheduledExecutorService.class),
                props, Mockito.mock(OutboxDlqTransfer.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OutboxProperties.DlqProperties is null");
    }

    @Test
    @DisplayName("UT outboxDlqCache returns Passthrough cache when gaugeProperties null")
    void outboxDlqCache_gaugeNull_returnsPassthrough() {
        // given
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        OutboxPublisherProperties.MetricsProperties metrics = new OutboxPublisherProperties.MetricsProperties();
        metrics.setGauge(null);
        dlq.setMetrics(metrics);
        props.setDlq(dlq);

        // when
        OutboxCache<?> cache = config.outboxDlqCache(props);

        // then
        assertThat(cache).isInstanceOf(PassthroughOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxDlqCache returns Passthrough cache when gauge disabled")
    void outboxDlqCache_gaugeDisabled_returnsPassthrough() {
        // given
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        OutboxPublisherProperties.MetricsProperties metrics = new OutboxPublisherProperties.MetricsProperties();
        OutboxPublisherProperties.MetricsProperties.GaugeProperties gauge = new OutboxPublisherProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(false);
        metrics.setGauge(gauge);
        dlq.setMetrics(metrics);
        props.setDlq(dlq);

        // when
        OutboxCache<?> cache = config.outboxDlqCache(props);

        // then
        assertThat(cache).isInstanceOf(PassthroughOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxDlqCache returns SimpleOutboxCache when ttls size == 3 and enabled")
    void outboxDlqCache_validTtls_returnsSimpleCache() {
        // given
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        OutboxPublisherProperties.MetricsProperties metrics = new OutboxPublisherProperties.MetricsProperties();
        OutboxPublisherProperties.MetricsProperties.GaugeProperties gauge = new OutboxPublisherProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);

        OutboxPublisherProperties.MetricsProperties.GaugeProperties.CacheProperties cacheProps =
                new OutboxPublisherProperties.MetricsProperties.GaugeProperties.CacheProperties();
        cacheProps.setTtls(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)));
        gauge.setCache(cacheProps);
        metrics.setGauge(gauge);
        dlq.setMetrics(metrics);
        props.setDlq(dlq);

        // when
        OutboxCache<?> cache = config.outboxDlqCache(props);

        // then
        assertThat(cache).isInstanceOf(SimpleOutboxCache.class);
    }
}
