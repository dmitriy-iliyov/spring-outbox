package io.github.dmitriyiliyov.oncebox.starter.consumer;

import io.github.dmitriyiliyov.oncebox.core.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.consumer.DefaultOutboxIdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.core.consumer.cache.OutboxIdempotentConsumerCacheDecorator;
import io.github.dmitriyiliyov.oncebox.metrics.consumer.OutboxIdempotentConsumerMetricsDecorator;
import io.github.dmitriyiliyov.oncebox.starter.OutboxRepositoryFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * The consumer decorator chain is built by {@code primaryOutboxIdempotentConsumer} from an injected
 * {@code List<OutboxIdempotentConsumerDecoratorSupplier>}, so its nesting depends entirely on the
 * {@code @Order} values of the supplier beans. Nothing about that ordering is observable through the
 * emitted metrics, which is why it is asserted structurally here.
 */
class OutboxIdempotentConsumerDecoratorChainOrderTests {

    private final OutboxConsumerProperties properties = mock(OutboxConsumerProperties.class);
    private final CacheManager cacheManager = mock(CacheManager.class);

    private ApplicationContextRunner contextRunner() {
        OutboxConsumerProperties.CacheProperties cache = mock(OutboxConsumerProperties.CacheProperties.class);
        when(cache.getCacheName()).thenReturn("cache-name");
        when(properties.getCache()).thenReturn(cache);
        when(cacheManager.getCache("cache-name")).thenReturn(mock(Cache.class));

        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        OutboxRepositoryFactory repositoryFactory = mock(OutboxRepositoryFactory.class);
        when(repositoryFactory.createConsumedOutboxRepository()).thenReturn(mock(ConsumedOutboxRepository.class));

        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OutboxConsumerAutoConfiguration.class))
                .withUserConfiguration(ProbeConfig.class)
                .withBean(OutboxConsumerProperties.class, () -> properties)
                .withBean(CacheManager.class, () -> cacheManager)
                .withBean(TransactionTemplate.class, () -> transactionTemplate)
                .withBean(OutboxRepositoryFactory.class, () -> repositoryFactory)
                .withBean(Clock.class, Clock::systemUTC)
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new);
    }

    @Test
    @DisplayName("IT should inject decorator suppliers ordered cache first, metrics second")
    void shouldInjectSuppliersInOrder() {
        contextRunner()
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.cache.enabled=true",
                        "oncebox.consumer.metrics.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(Probe.class).suppliers())
                            .extracting(Object::getClass)
                            .containsExactly(
                                    OutboxIdempotentConsumerCacheDecoratorSupplier.class,
                                    OutboxIdempotentConsumerMetricsDecoratorSupplier.class
                            );
                });
    }

    @Test
    @DisplayName("IT should nest the primary consumer as metrics -> cache -> default")
    void shouldNestMetricsOutsideCache() {
        contextRunner()
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.cache.enabled=true",
                        "oncebox.consumer.metrics.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    OutboxIdempotentConsumer primary = context.getBean(OutboxIdempotentConsumer.class);

                    assertThat(primary).isInstanceOf(OutboxIdempotentConsumerMetricsDecorator.class);
                    OutboxIdempotentConsumer inner = delegateOf(primary);
                    assertThat(inner).isInstanceOf(OutboxIdempotentConsumerCacheDecorator.class);
                    assertThat(delegateOf(inner)).isInstanceOf(DefaultOutboxIdempotentConsumer.class);
                });
    }

    @Test
    @DisplayName("IT should nest only the cache decorator when metrics are disabled")
    void shouldNestOnlyCacheWhenMetricsDisabled() {
        contextRunner()
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.cache.enabled=true",
                        "oncebox.consumer.metrics.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    OutboxIdempotentConsumer primary = context.getBean(OutboxIdempotentConsumer.class);

                    assertThat(primary).isInstanceOf(OutboxIdempotentConsumerCacheDecorator.class);
                    assertThat(delegateOf(primary)).isInstanceOf(DefaultOutboxIdempotentConsumer.class);
                });
    }

    @Test
    @DisplayName("IT should nest only the metrics decorator when the cache is disabled")
    void shouldNestOnlyMetricsWhenCacheDisabled() {
        contextRunner()
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.cache.enabled=false",
                        "oncebox.consumer.metrics.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    OutboxIdempotentConsumer primary = context.getBean(OutboxIdempotentConsumer.class);

                    assertThat(primary).isInstanceOf(OutboxIdempotentConsumerMetricsDecorator.class);
                    assertThat(delegateOf(primary)).isInstanceOf(DefaultOutboxIdempotentConsumer.class);
                });
    }

    @Test
    @DisplayName("IT should leave the primary consumer undecorated when cache and metrics are disabled")
    void shouldNotDecorateWhenCacheAndMetricsDisabled() {
        contextRunner()
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.cache.enabled=false",
                        "oncebox.consumer.metrics.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(OutboxIdempotentConsumer.class))
                            .isInstanceOf(DefaultOutboxIdempotentConsumer.class);
                });
    }

    private static OutboxIdempotentConsumer delegateOf(OutboxIdempotentConsumer decorator) {
        return (OutboxIdempotentConsumer) ReflectionTestUtils.getField(decorator, "delegate");
    }

    record Probe(List<OutboxIdempotentConsumerDecoratorSupplier> suppliers) { }

    @Configuration
    static class ProbeConfig {
        @Bean
        Probe probe(List<OutboxIdempotentConsumerDecoratorSupplier> suppliers) {
            return new Probe(suppliers);
        }
    }
}
