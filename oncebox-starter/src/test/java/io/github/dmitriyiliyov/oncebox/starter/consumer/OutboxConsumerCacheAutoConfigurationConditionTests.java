package io.github.dmitriyiliyov.oncebox.starter.consumer;

import io.github.dmitriyiliyov.oncebox.consumer.cache.DefaultConsumedOutboxCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxConsumerCacheAutoConfigurationConditionTests {

    private final OutboxConsumerProperties properties = mock(OutboxConsumerProperties.class);
    private final CacheManager cacheManager = mock(CacheManager.class);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OutboxConsumerCacheAutoConfiguration.class))
            .withBean(OutboxConsumerProperties.class, () -> properties)
            .withBean(CacheManager.class, () -> cacheManager);

    @Test
    @DisplayName("IT should load configuration when all conditions are met")
    void shouldLoadWhenAllConditionsMet() {
        OutboxConsumerProperties.CacheProperties cache = mock(OutboxConsumerProperties.CacheProperties.class);
        when(cache.getCacheName()).thenReturn("cache-name");
        when(properties.getCache()).thenReturn(cache);
        when(cacheManager.getCache("cache-name")).thenReturn(mock(Cache.class));
        contextRunner
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.cache.enabled=true"
                )
                .run(context -> assertThat(context).hasSingleBean(OutboxConsumerCacheAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when outbox.consumer.enabled is false")
    void shouldNotLoadWhenConsumerEnabledIsFalse() {
        contextRunner
                .withPropertyValues(
                        "oncebox.consumer.enabled=false",
                        "oncebox.consumer.cache.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(OutboxConsumerCacheAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when outbox.consumer.enabled is missing")
    void shouldNotLoadWhenConsumerEnabledIsMissing() {
        contextRunner
                .withPropertyValues(
                        "oncebox.consumer.cache.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(OutboxConsumerCacheAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when outbox.consumer.cache.enabled is false")
    void shouldNotLoadWhenCacheEnabledIsFalse() {
        contextRunner
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.cache.enabled=false"
                )
                .run(context -> assertThat(context).doesNotHaveBean(OutboxConsumerCacheAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when outbox.consumer.cache.enabled is missing")
    void shouldNotLoadWhenCacheEnabledIsMissing() {
        contextRunner
                .withPropertyValues(
                        "oncebox.consumer.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(OutboxConsumerCacheAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when DefaultConsumedOutboxCache class is missing on classpath")
    void shouldNotLoadWhenRequiredClassIsMissing() {
        contextRunner
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.cache.enabled=true"
                )
                .withClassLoader(new FilteredClassLoader(DefaultConsumedOutboxCache.class))
                .run(context -> assertThat(context).doesNotHaveBean(OutboxConsumerCacheAutoConfiguration.class));
    }
}