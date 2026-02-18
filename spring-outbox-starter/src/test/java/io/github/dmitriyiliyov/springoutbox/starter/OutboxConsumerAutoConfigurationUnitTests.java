package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxEventIdResolveManager;
import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxEventIdResolver;
import io.github.dmitriyiliyov.springoutbox.metrics.consumer.ConsumedOutboxManagerMetricsDecorator;
import io.github.dmitriyiliyov.springoutbox.metrics.consumer.OutboxIdempotentConsumerMetricsDecorator;
import io.github.dmitriyiliyov.springoutbox.starter.consumer.OutboxConsumerAutoConfiguration;
import io.github.dmitriyiliyov.springoutbox.starter.consumer.OutboxConsumerProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxConsumerAutoConfigurationUnitTests {

    @Mock
    OutboxConsumerProperties properties;

    @InjectMocks
    OutboxConsumerAutoConfiguration config;

    @Mock
    CacheManager cacheManager;

    @Mock
    ConsumedOutboxRepository repository;

    @Mock
    MeterRegistry registry;

    @Mock
    OutboxConsumerProperties.CacheProperties cacheProperties;

    @Test
    @DisplayName("UT consumedOutboxManager() when cacheManager null should return metrics decorator")
    void consumedOutboxManager_whenCacheManagerNull_shouldReturnMetricsDecorator() {
        // given
        when(properties.getCache()).thenReturn(cacheProperties);

        // when
        ConsumedOutboxManager manager = config.consumedOutboxManager(null, repository, registry);

        // then
        assertThat(manager).isInstanceOf(ConsumedOutboxManagerMetricsDecorator.class);
    }

    @Test
    @DisplayName("UT consumedOutboxManager() when cache disabled should return metrics decorator")
    void consumedOutboxManager_whenCacheDisabled_shouldReturnMetricsDecorator() {
        // given
        when(properties.getCache()).thenReturn(cacheProperties);
        when(cacheProperties.isEnabled()).thenReturn(false);

        // when
        ConsumedOutboxManager manager = config.consumedOutboxManager(cacheManager, repository, registry);

        // then
        assertThat(manager).isInstanceOf(ConsumedOutboxManagerMetricsDecorator.class);
    }

    @Test
    @DisplayName("UT consumedOutboxManager() when cache enabled should return cache decorator")
    void consumedOutboxManager_whenCacheEnabled_shouldReturnCacheDecorator() {
        // given
        String cacheName = "myCache";
        when(properties.getCache()).thenReturn(cacheProperties);
        when(cacheProperties.isEnabled()).thenReturn(true);
        when(cacheProperties.getCacheName()).thenReturn(cacheName);
        when(cacheManager.getCache(cacheName)).thenReturn(mock(Cache.class));

        // when
        ConsumedOutboxManager manager = config.consumedOutboxManager(cacheManager, repository, registry);

        // then
        assertThat(manager).isInstanceOf(ConsumedOutboxManagerMetricsDecorator.class);
    }

    @Test
    @DisplayName("UT consumedOutboxManager() when metrics enabled should use MetricsConsumedOutboxCacheObserver")
    void consumedOutboxManager_whenMetricsEnabled_shouldUseMetricsObserver() {
        // given
        String cacheName = "myCache";
        when(properties.getCache()).thenReturn(cacheProperties);
        when(cacheProperties.isEnabled()).thenReturn(true);
        when(cacheProperties.getCacheName()).thenReturn(cacheName);
        when(cacheManager.getCache(cacheName)).thenReturn(mock(Cache.class));

        OutboxProperties.MetricsProperties metrics = mock(OutboxProperties.MetricsProperties.class);
        when(properties.getMetrics()).thenReturn(metrics);
        when(metrics.isEnabled()).thenReturn(true);

        // when
        ConsumedOutboxManager manager = config.consumedOutboxManager(cacheManager, repository, registry);

        // then
        assertThat(manager).isInstanceOf(ConsumedOutboxManagerMetricsDecorator.class);
    }

    @Test
    @DisplayName("UT consumedOutboxManager() when metrics disabled should use NoopConsumedOutboxCacheObserver")
    void consumedOutboxManager_whenMetricsDisabled_shouldUseNoopObserver() {
        // given
        String cacheName = "myCache";
        when(properties.getCache()).thenReturn(cacheProperties);
        when(cacheProperties.isEnabled()).thenReturn(true);
        when(cacheProperties.getCacheName()).thenReturn(cacheName);
        when(cacheManager.getCache(cacheName)).thenReturn(mock(Cache.class));

        OutboxProperties.MetricsProperties metrics = mock(OutboxProperties.MetricsProperties.class);
        when(properties.getMetrics()).thenReturn(metrics);
        when(metrics.isEnabled()).thenReturn(false);

        // when
        ConsumedOutboxManager manager = config.consumedOutboxManager(cacheManager, repository, registry);

        // then
        assertThat(manager).isInstanceOf(ConsumedOutboxManagerMetricsDecorator.class);
    }

    @Test
    @DisplayName("UT consumedOutboxManager() when metrics null should use NoopConsumedOutboxCacheObserver")
    void consumedOutboxManager_whenMetricsNull_shouldUseNoopObserver() {
        // given
        String cacheName = "myCache";
        when(properties.getCache()).thenReturn(cacheProperties);
        when(cacheProperties.isEnabled()).thenReturn(true);
        when(cacheProperties.getCacheName()).thenReturn(cacheName);
        when(cacheManager.getCache(cacheName)).thenReturn(mock(Cache.class));

        when(properties.getMetrics()).thenReturn(null);

        // when
        ConsumedOutboxManager manager = config.consumedOutboxManager(cacheManager, repository, registry);

        // then
        assertThat(manager).isInstanceOf(ConsumedOutboxManagerMetricsDecorator.class);
    }

    @Test
    @DisplayName("UT outboxIdempotentConsumer() when metrics disabled should return default consumer")
    void outboxIdempotentConsumer_whenMetricsDisabled_shouldReturnDefault() {
        // given
        OutboxProperties.MetricsProperties metrics = mock(OutboxProperties.MetricsProperties.class);
        when(properties.getMetrics()).thenReturn(metrics);
        when(metrics.isEnabled()).thenReturn(false);

        OutboxEventIdResolveManager idResolver = mock(OutboxEventIdResolveManager.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        ConsumedOutboxManager manager = mock(ConsumedOutboxManager.class);

        // when
        var consumer = config.outboxIdempotentConsumer(idResolver, transactionTemplate, manager, registry);

        // then
        assertThat(consumer).isNotInstanceOf(OutboxIdempotentConsumerMetricsDecorator.class);
    }

    @Test
    @DisplayName("UT outboxIdempotentConsumer() when metrics null should return default consumer")
    void outboxIdempotentConsumer_whenMetricsNull_shouldReturnDefault() {
        // given
        when(properties.getMetrics()).thenReturn(null);

        OutboxEventIdResolveManager idResolver = mock(OutboxEventIdResolveManager.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        ConsumedOutboxManager manager = mock(ConsumedOutboxManager.class);

        // when
        var consumer = config.outboxIdempotentConsumer(idResolver, transactionTemplate, manager, registry);

        // then
        assertThat(consumer).isNotInstanceOf(OutboxIdempotentConsumerMetricsDecorator.class);
    }

    @Test
    @DisplayName("UT outboxIdempotentConsumer() when metrics enabled should return metrics decorator")
    void outboxIdempotentConsumer_whenMetricsEnabled_shouldReturnDecorator() {
        // given
        OutboxProperties.MetricsProperties metrics = mock(OutboxProperties.MetricsProperties.class);
        when(properties.getMetrics()).thenReturn(metrics);
        when(metrics.isEnabled()).thenReturn(true);

        OutboxEventIdResolveManager idResolver = mock(OutboxEventIdResolveManager.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        ConsumedOutboxManager manager = mock(ConsumedOutboxManager.class);

        // when
        var consumer = config.outboxIdempotentConsumer(idResolver, transactionTemplate, manager, registry);

        // then
        assertThat(consumer).isInstanceOf(OutboxIdempotentConsumerMetricsDecorator.class);
    }

    @Test
    @DisplayName("UT defaultOutboxEventIdResolveManager() when resolvers empty should throw")
    void defaultOutboxEventIdResolveManager_whenResolversEmpty_shouldThrow() {
        // given
        List<OutboxEventIdResolver<?>> resolvers = List.of();

        // when + then
        assertThrows(IllegalArgumentException.class, () -> config.defaultOutboxEventIdResolveManager(resolvers));
    }

    @Test
    @DisplayName("UT defaultOutboxEventIdResolveManager() when resolvers present should create manager")
    void defaultOutboxEventIdResolveManager_whenResolversPresent_shouldCreateManager() {
        // given
        OutboxEventIdResolver<?> resolver = mock(OutboxEventIdResolver.class);
        List<OutboxEventIdResolver<?>> resolvers = List.of(resolver);

        // when
        OutboxEventIdResolveManager manager = config.defaultOutboxEventIdResolveManager(resolvers);

        // then
        assertThat(manager).isNotNull();
    }
}
