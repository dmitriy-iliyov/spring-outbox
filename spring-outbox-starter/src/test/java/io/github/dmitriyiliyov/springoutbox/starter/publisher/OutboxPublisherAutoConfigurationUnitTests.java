package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.publisher.*;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.NoopOutboxCache;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.OutboxCache;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.SimpleOutboxCache;
import io.github.dmitriyiliyov.springoutbox.starter.BeanNameUtils;
import io.github.dmitriyiliyov.springoutbox.starter.OutboxProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherAutoConfigurationUnitTests {

    private OutboxPublisherProperties props;
    private OutboxPublisherAutoConfiguration config;

    @Mock
    private OutboxPublisherProperties mockedPublisherProperties;

    @Mock
    private ScheduledExecutorService executor;

    @Mock
    private OutboxProcessor processor;

    @Mock
    private OutboxManager manager;

    @Mock
    private ConfigurableListableBeanFactory factory;

    @Mock
    private OutboxPublisherProperties.EventProperties eventPropertiesHolder;

    @BeforeEach
    void setUp() {
        props = new OutboxPublisherProperties();
        config = new OutboxPublisherAutoConfiguration(props, mock(ObjectMapper.class));
    }

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

//    @Test
//    @DisplayName("UT outboxManager returns default manager when metrics disabled")
//    void outboxManager_metricsDisabled_returnsDefault() {
//        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
//        metrics.setEnabled(false);
//        props.setMetrics(metrics);
//
//        OutboxRepository repository = mock(OutboxRepository.class);
//        MeterRegistry registry = mock(MeterRegistry.class);
//
//        OutboxManager manager = config.outboxManager(repository, registry);
//
//        assertThat(manager).isInstanceOf(DefaultOutboxManager.class);
//    }
//
//    @Test
//    @DisplayName("UT outboxManager returns default manager when metrics null")
//    void outboxManager_metricsNull_returnsDefault() {
//        props.setMetrics(null);
//
//        OutboxRepository repository = mock(OutboxRepository.class);
//        MeterRegistry registry = mock(MeterRegistry.class);
//
//        OutboxManager manager = config.outboxManager(repository, registry);
//
//        assertThat(manager).isInstanceOf(DefaultOutboxManager.class);
//    }
//
//    @Test
//    @DisplayName("UT outboxManager returns metrics decorator when metrics enabled")
//    void outboxManager_metricsEnabled_returnsDecorator() {
//        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
//        metrics.setEnabled(true);
//        props.setMetrics(metrics);
//        props.setEvents(Map.of());
//
//        OutboxRepository repository = mock(OutboxRepository.class);
//        MeterRegistry registry = mock(MeterRegistry.class);
//        when(registry.counter(anyString(), anyString(), anyString())).thenReturn(mock(Counter.class));
//
//        OutboxManager manager = config.outboxManager(repository, registry);
//
//        assertThat(manager).isInstanceOf(OutboxManagerMetricsDecorator.class);
//    }

    @Test
    @DisplayName("UT outboxSchedulersInitializer should register publisher and recovery schedulers when cleanup is disabled")
    void shouldRegisterPublisherAndRecoverySchedulers() {
        when(mockedPublisherProperties.getEvents()).thenReturn(Map.of("testEvent", eventPropertiesHolder));
        when(eventPropertiesHolder.getEventType()).thenReturn("test-event");
        when(mockedPublisherProperties.isCleanUpEnabled()).thenReturn(false);
        when(factory.containsBean(anyString())).thenReturn(false);

        try (MockedStatic<BeanNameUtils> beanNameUtils = mockStatic(BeanNameUtils.class)) {
            beanNameUtils.when(() -> BeanNameUtils.toBeanName("test-event", "OutboxPublisherScheduler"))
                    .thenReturn("testEventOutboxPublisherScheduler");

            SmartInitializingSingleton initializer = config.outboxSchedulersInitializer(
                    mockedPublisherProperties, executor, processor, manager, factory
            );

            initializer.afterSingletonsInstantiated();

            verify(factory).registerSingleton(eq("testEventOutboxPublisherScheduler"), any(OutboxPollingScheduler.class));
            verify(factory).registerSingleton(eq("outboxRecoveryScheduler"), any(OutboxRecoveryScheduler.class));
            verify(factory, never()).registerSingleton(eq("outboxCleanUpScheduler"), any());
        }
    }

    @Test
    @DisplayName("UT outboxSchedulersInitializer should skip publisher scheduler registration if bean already exists")
    void shouldSkipPublisherSchedulerIfBeanExists() {
        when(mockedPublisherProperties.getEvents()).thenReturn(Map.of("testEvent", eventPropertiesHolder));
        when(eventPropertiesHolder.getEventType()).thenReturn("test-event");
        when(mockedPublisherProperties.isCleanUpEnabled()).thenReturn(false);

        try (MockedStatic<BeanNameUtils> beanNameUtils = mockStatic(BeanNameUtils.class)) {
            beanNameUtils.when(() -> BeanNameUtils.toBeanName("test-event", "OutboxPublisherScheduler"))
                    .thenReturn("testEventOutboxPublisherScheduler");

            when(factory.containsBean("testEventOutboxPublisherScheduler")).thenReturn(true);

            SmartInitializingSingleton initializer = config.outboxSchedulersInitializer(
                    mockedPublisherProperties, executor, processor, manager, factory
            );

            initializer.afterSingletonsInstantiated();

            verify(factory, never()).registerSingleton(eq("testEventOutboxPublisherScheduler"), any(OutboxPollingScheduler.class));
            verify(factory).registerSingleton(eq("outboxRecoveryScheduler"), any(OutboxRecoveryScheduler.class));
        }
    }

    @Test
    @DisplayName("UT outboxSchedulersInitializer should register cleanup scheduler when cleanup is enabled and properties are present")
    void shouldRegisterCleanUpSchedulerWhenEnabled() {
        when(mockedPublisherProperties.getEvents()).thenReturn(Map.of());
        when(mockedPublisherProperties.isCleanUpEnabled()).thenReturn(true);
        when(mockedPublisherProperties.getCleanUp()).thenReturn(mock(OutboxProperties.CleanUpProperties.class));

        SmartInitializingSingleton initializer = config.outboxSchedulersInitializer(
                mockedPublisherProperties, executor, processor, manager, factory
        );

        initializer.afterSingletonsInstantiated();

        verify(factory).registerSingleton(eq("outboxRecoveryScheduler"), any(OutboxRecoveryScheduler.class));
        verify(factory).registerSingleton(eq("outboxCleanUpScheduler"), any(OutboxCleanUpScheduler.class));
    }

    @Test
    @DisplayName("UT outboxSchedulersInitializer should throw IllegalStateException when cleanup is enabled but properties are null")
    void shouldThrowExceptionWhenCleanUpPropertiesAreNull() {
        when(mockedPublisherProperties.getEvents()).thenReturn(Map.of());
        when(mockedPublisherProperties.isCleanUpEnabled()).thenReturn(true);
        when(mockedPublisherProperties.getCleanUp()).thenReturn(null);

        SmartInitializingSingleton initializer = config.outboxSchedulersInitializer(
                mockedPublisherProperties, executor, processor, manager, factory
        );

        assertThatThrownBy(initializer::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OutboxProperties.CleanUpProperties is null");

        verify(factory).registerSingleton(eq("outboxRecoveryScheduler"), any(OutboxRecoveryScheduler.class));
        verify(factory, never()).registerSingleton(eq("outboxCleanUpScheduler"), any());
    }
}