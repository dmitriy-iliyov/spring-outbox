package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.springoutbox.core.polling.OutboxScheduleStrategy;
import io.github.dmitriyiliyov.springoutbox.core.publisher.*;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.NoopOutboxCache;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.OutboxCache;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.SimpleOutboxCache;
import io.github.dmitriyiliyov.springoutbox.starter.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private ScheduledExecutorService executor;

    @Mock
    private OutboxProcessor processor;

    @Mock
    private ConfigurableListableBeanFactory factory;

    @Mock
    private OutboxPublisherProperties.EventProperties eventPropertiesHolder;

    @Mock
    private OutboxScheduleStrategyListenerSupplier scheduleStrategyListenerSupplier;

    @Mock
    private ContinuableTaskDecoratorSupplier continuableTaskDecoratorSupplier;

    @Mock
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        props = mock(OutboxPublisherProperties.class);
        config = new OutboxPublisherAutoConfiguration(props, mapper);
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
    @DisplayName("UT outboxCache throws when ttls null or empty")
    void outboxCache_ttlsNullOrEmpty_throws() {
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();
        OutboxProperties.MetricsProperties.GaugeProperties gauge = new OutboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        gauge.setCache(new OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties());
        metrics.setGauge(gauge);
        when(props.getMetrics()).thenReturn(metrics);

        assertThatThrownBy(config::outboxCache)
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
        when(props.getMetrics()).thenReturn(metrics);

        assertThatThrownBy(config::outboxCache)
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
        when(props.getMetrics()).thenReturn(metrics);

        OutboxCache<?> cache = config.outboxCache();

        assertThat(cache).isInstanceOf(SimpleOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxSchedulersInitializer should skip publisher scheduler registration if bean already exists")
    void shouldSkipPublisherSchedulerIfBeanExists() {
        when(props.getEvents()).thenReturn(Map.of("testEvent", eventPropertiesHolder));
        when(eventPropertiesHolder.getEventType()).thenReturn("test-event");

        try (MockedStatic<BeanNameUtils> beanNameUtils = mockStatic(BeanNameUtils.class)) {
            beanNameUtils.when(() -> BeanNameUtils.toBeanName("test-event", "OutboxPublisherScheduler"))
                    .thenReturn("testEventOutboxPublisherScheduler");

            when(factory.containsBean("testEventOutboxPublisherScheduler")).thenReturn(true);

            SmartInitializingSingleton initializer = config.outboxSchedulersInitializer(
                    executor, processor, factory, scheduleStrategyListenerSupplier, continuableTaskDecoratorSupplier
            );

            initializer.afterSingletonsInstantiated();

            verify(factory, never()).registerSingleton(eq("testEventOutboxPublisherScheduler"), any(OutboxPollingScheduler.class));
        }
    }

    @Test
    @DisplayName("UT outboxSchedulersInitializer should register publisher scheduler if bean does not exist")
    void shouldRegisterPublisherSchedulerIfBeanDoesNotExist() {
        when(props.getEvents()).thenReturn(Map.of("testEvent", eventPropertiesHolder));
        when(eventPropertiesHolder.getEventType()).thenReturn("test-event");
        when(eventPropertiesHolder.getPolling()).thenReturn(mock(OutboxProperties.PollingProperties.class));
        when(continuableTaskDecoratorSupplier.supply(anyString())).thenReturn(mock(ContinuableTaskDecorator.class));

        try (MockedStatic<BeanNameUtils> beanNameUtils = mockStatic(BeanNameUtils.class);
             MockedStatic<OutboxScheduleStrategyFactory> strategyFactory = mockStatic(OutboxScheduleStrategyFactory.class)) {

            beanNameUtils.when(() -> BeanNameUtils.toBeanName("test-event", "OutboxPublisherScheduler"))
                    .thenReturn("testEventOutboxPublisherScheduler");
            strategyFactory.when(() -> OutboxScheduleStrategyFactory.create(any(), any(), any(), any()))
                    .thenReturn(mock(OutboxScheduleStrategy.class));
            when(factory.containsBean("testEventOutboxPublisherScheduler")).thenReturn(false);

            SmartInitializingSingleton initializer = config.outboxSchedulersInitializer(
                    executor, processor, factory, scheduleStrategyListenerSupplier, continuableTaskDecoratorSupplier
            );

            initializer.afterSingletonsInstantiated();

            verify(factory).registerSingleton(eq("testEventOutboxPublisherScheduler"), any(OutboxPollingScheduler.class));
        }
    }

    @Test
    @DisplayName("UT outboxRecoveryScheduler creates OutboxRecoveryScheduler")
    void outboxRecoveryScheduler_createsScheduler(@Mock OutboxManager manager) {
        OutboxPublisherProperties.StuckRecoveryProperties stuckRecoveryProperties = mock(OutboxPublisherProperties.StuckRecoveryProperties.class);
        when(stuckRecoveryProperties.getPolling()).thenReturn(mock(OutboxProperties.PollingProperties.class));
        when(props.getStuckRecovery()).thenReturn(stuckRecoveryProperties);
        when(continuableTaskDecoratorSupplier.supply(anyString())).thenReturn(mock(ContinuableTaskDecorator.class));

        try (MockedStatic<OutboxScheduleStrategyFactory> strategyFactory = mockStatic(OutboxScheduleStrategyFactory.class)) {
            strategyFactory.when(() -> OutboxScheduleStrategyFactory.create(any(), any(), any(), any()))
                    .thenReturn(mock(OutboxScheduleStrategy.class));

            OutboxScheduler scheduler = config.outboxRecoveryScheduler(
                    executor, scheduleStrategyListenerSupplier, manager, continuableTaskDecoratorSupplier
            );

            assertThat(scheduler).isInstanceOf(OutboxRecoveryScheduler.class);
        }
    }

    @Test
    @DisplayName("UT outboxCleanUpScheduler creates OutboxCleanUpScheduler")
    void outboxCleanUpScheduler_createsScheduler(@Mock OutboxProperties outboxProperties,
                                                 @Mock OutboxManager manager,
                                                 @Mock DistributedLockRepository lockRepository) {
        OutboxProperties.CleanUpProperties cleanUpProperties = mock(OutboxProperties.CleanUpProperties.class);
        when(cleanUpProperties.getPolling()).thenReturn(mock(OutboxProperties.PollingProperties.class));
        when(props.getCleanUp()).thenReturn(cleanUpProperties);
        when(outboxProperties.getWorkerId()).thenReturn(UUID.randomUUID());
        when(continuableTaskDecoratorSupplier.supply(anyString())).thenReturn(mock(ContinuableTaskDecorator.class));

        try (MockedStatic<OutboxScheduleStrategyFactory> strategyFactory = mockStatic(OutboxScheduleStrategyFactory.class)) {
            strategyFactory.when(() -> OutboxScheduleStrategyFactory.create(any(), any(), any(), any()))
                    .thenReturn(mock(OutboxScheduleStrategy.class));

            OutboxScheduler scheduler = config.outboxCleanUpScheduler(
                    outboxProperties, executor, scheduleStrategyListenerSupplier, manager, lockRepository, continuableTaskDecoratorSupplier
            );

            assertThat(scheduler).isInstanceOf(OutboxCleanUpScheduler.class);
        }
    }

    @Test
    @DisplayName("UT outboxCleanUpJobCreateCommand creates DefaultOutboxJobCreateCommand")
    void outboxCleanUpJobCreateCommand_createsCommand(@Mock OutboxProperties outboxProperties,
                                                      @Mock JdbcTemplate jdbcTemplate,
                                                      @Mock Clock clock) {
        OutboxProperties.CleanUpProperties cleanUpProperties = mock(OutboxProperties.CleanUpProperties.class);
        when(cleanUpProperties.getPolling()).thenReturn(mock(OutboxProperties.PollingProperties.class));
        when(props.getCleanUp()).thenReturn(cleanUpProperties);
        when(outboxProperties.getDistributedLock()).thenReturn(mock(OutboxProperties.DistributedLockProperties.class));

        try (MockedStatic<DistributedLockPropertiesResolver> resolver = mockStatic(DistributedLockPropertiesResolver.class)) {
            DistributedLockPropertiesResolver.LockDurations lockDurations = mock(DistributedLockPropertiesResolver.LockDurations.class);
            when(lockDurations.atLeastFor()).thenReturn(Duration.ofSeconds(1).toMillis());
            when(lockDurations.atMostFor()).thenReturn(Duration.ofSeconds(5).toMillis());
            resolver.when(() -> DistributedLockPropertiesResolver.resolve(any(), any())).thenReturn(lockDurations);

            OutboxJobCreateCommand command = config.outboxCleanUpJobCreateCommand(outboxProperties, jdbcTemplate, clock);

            assertThat(command).isInstanceOf(DefaultOutboxJobCreateCommand.class);
        }
    }
}