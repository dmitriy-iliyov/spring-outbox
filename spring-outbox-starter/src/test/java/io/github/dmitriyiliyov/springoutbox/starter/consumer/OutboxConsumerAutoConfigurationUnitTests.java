package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxEventIdResolveManager;
import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxEventIdResolver;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class OutboxConsumerAutoConfigurationUnitTests {

    @Mock
    OutboxConsumerProperties properties;

    @InjectMocks
    OutboxConsumerAutoConfiguration config;

    @Mock
    CacheManager cacheManager;

    @Mock
    MeterRegistry registry;

    @Mock
    OutboxConsumerProperties.CacheProperties cacheProperties;

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
