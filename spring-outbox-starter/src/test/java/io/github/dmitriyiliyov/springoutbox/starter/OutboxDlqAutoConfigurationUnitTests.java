package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.*;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.dlq.OutboxDlqManagerMetricsDecorator;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.dlq.OutboxDlqTransferMetricsDecorator;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.NoopOutboxCache;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.OutboxCache;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.SimpleOutboxCache;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxDlqAutoConfiguration;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxPublisherProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        assertThat(cache).isInstanceOf(NoopOutboxCache.class).isNotNull();
    }

    @Test
    @DisplayName("UT outboxDlqCache throws when ttls null or empty")
    void outboxDlqCache_ttlsNull_throws() {
        // given
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        props.setDlq(dlq);

        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        metrics.setEnabled(true);
        OutboxProperties.MetricsProperties.GaugeProperties gauge = new OutboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        gauge.setCache(new OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties());
        gauge.getCache().setEnabled(true);
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
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        metrics.setEnabled(true);
        OutboxProperties.MetricsProperties.GaugeProperties gauge = new OutboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        gauge.setCache(new OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties());
        gauge.getCache().setEnabled(true);
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
    @DisplayName("UT outboxDlqCache returns Passthrough cache when gaugeProperties null")
    void outboxDlqCache_gaugeNull_returnsPassthrough() {
        // given
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        metrics.setEnabled(true);
        metrics.setGauge(null);
        dlq.setMetrics(metrics);
        props.setDlq(dlq);

        // when
        OutboxCache<?> cache = config.outboxDlqCache(props);

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
        dlq.setMetrics(metrics);
        props.setDlq(dlq);

        // when
        OutboxCache<?> cache = config.outboxDlqCache(props);

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
        dlq.setMetrics(metrics);
        props.setDlq(dlq);

        // when
        OutboxCache<?> cache = config.outboxDlqCache(props);

        // then
        assertThat(cache).isInstanceOf(SimpleOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxDlqManager returns default manager when metrics disabled")
    void outboxDlqManager_metricsDisabled_returnsDefault() {
        // given
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        metrics.setEnabled(false);
        dlq.setMetrics(metrics);
        props.setDlq(dlq);

        OutboxDlqRepository repository = mock(OutboxDlqRepository.class);
        MeterRegistry registry = mock(MeterRegistry.class);

        // when
        OutboxDlqManager manager = config.outboxDlqManager(repository, props, registry);

        // then
        assertThat(manager).isInstanceOf(DefaultOutboxDlqManager.class);
    }

    @Test
    @DisplayName("UT outboxDlqManager returns default manager when metrics null")
    void outboxDlqManager_metricsNull_returnsDefault() {
        // given
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        dlq.setMetrics(null);
        props.setDlq(dlq);

        OutboxDlqRepository repository = mock(OutboxDlqRepository.class);
        MeterRegistry registry = mock(MeterRegistry.class);

        // when
        OutboxDlqManager manager = config.outboxDlqManager(repository, props, registry);

        // then
        assertThat(manager).isInstanceOf(DefaultOutboxDlqManager.class);
    }

    @Test
    @DisplayName("UT outboxDlqManager returns metrics decorator when metrics enabled")
    void outboxDlqManager_metricsEnabled_returnsDecorator() {
        // given
        OutboxPublisherProperties props = mock(OutboxPublisherProperties.class);
        OutboxPublisherProperties.DlqProperties dlq = mock(OutboxPublisherProperties.DlqProperties.class);
        OutboxProperties.MetricsProperties metrics = mock(OutboxProperties.MetricsProperties.class);

        when(props.getDlq()).thenReturn(dlq);
        when(dlq.getMetrics()).thenReturn(metrics);
        when(metrics.isEnabled()).thenReturn(true);
        when(props.getEventHolders()).thenReturn(Map.of());

        OutboxDlqRepository repository = mock(OutboxDlqRepository.class);
        MeterRegistry registry = mock(MeterRegistry.class);
        when(registry.counter(anyString(), anyString(), anyString())).thenReturn(mock(Counter.class));

        // when
        OutboxDlqManager manager = config.outboxDlqManager(repository, props, registry);

        // then
        assertThat(manager).isInstanceOf(OutboxDlqManagerMetricsDecorator.class);
    }

    @Test
    @DisplayName("UT outboxDlqTransfer returns default transfer when metrics disabled")
    void outboxDlqTransfer_metricsDisabled_returnsDefault() {
        // given
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        metrics.setEnabled(false);
        dlq.setMetrics(metrics);
        props.setDlq(dlq);

        OutboxManager manager = mock(OutboxManager.class);
        OutboxDlqManager dlqManager = mock(OutboxDlqManager.class);
        OutboxDlqHandler handler = mock(OutboxDlqHandler.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        MeterRegistry registry = mock(MeterRegistry.class);

        // when
        OutboxDlqTransfer transfer = config.outboxDlqTransfer(manager, dlqManager, handler, transactionTemplate, props, registry);

        // then
        assertThat(transfer).isInstanceOf(DefaultOutboxDlqTransfer.class);
    }

    @Test
    @DisplayName("UT outboxDlqTransfer returns default transfer when metrics null")
    void outboxDlqTransfer_metricsNull_returnsDefault() {
        // given
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        dlq.setMetrics(null);
        props.setDlq(dlq);

        OutboxManager manager = mock(OutboxManager.class);
        OutboxDlqManager dlqManager = mock(OutboxDlqManager.class);
        OutboxDlqHandler handler = mock(OutboxDlqHandler.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        MeterRegistry registry = mock(MeterRegistry.class);

        // when
        OutboxDlqTransfer transfer = config.outboxDlqTransfer(manager, dlqManager, handler, transactionTemplate, props, registry);

        // then
        assertThat(transfer).isInstanceOf(DefaultOutboxDlqTransfer.class);
    }

    @Test
    @DisplayName("UT outboxDlqTransfer returns metrics decorator when metrics enabled")
    void outboxDlqTransfer_metricsEnabled_returnsDecorator() {
        // given
        OutboxPublisherProperties props = mock(OutboxPublisherProperties.class);
        OutboxPublisherProperties.DlqProperties dlq = mock(OutboxPublisherProperties.DlqProperties.class);
        OutboxProperties.MetricsProperties metrics = mock(OutboxProperties.MetricsProperties.class);

        when(props.getDlq()).thenReturn(dlq);
        when(dlq.getMetrics()).thenReturn(metrics);
        when(metrics.isEnabled()).thenReturn(true);

        OutboxManager manager = mock(OutboxManager.class);
        OutboxDlqManager dlqManager = mock(OutboxDlqManager.class);
        OutboxDlqHandler handler = mock(OutboxDlqHandler.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        MeterRegistry registry = mock(MeterRegistry.class);
        when(registry.timer(anyString())).thenReturn(mock(Timer.class));

        // when
        OutboxDlqTransfer transfer = config.outboxDlqTransfer(manager, dlqManager, handler, transactionTemplate, props, registry);

        // then
        assertThat(transfer).isInstanceOf(OutboxDlqTransferMetricsDecorator.class);
    }
}
