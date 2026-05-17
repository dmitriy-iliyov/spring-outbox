package io.github.dmitriyiliyov.springoutbox.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompositePostApplicationReadyOutboxInitializerUnitTests {

    @Test
    @DisplayName("UT init() when ONLY publisher is enabled should log publisher properties")
    void init_whenOnlyPublisherEnabled_shouldLogPublisherProperties() {
        OutboxProperties properties = mock(OutboxProperties.class, RETURNS_DEEP_STUBS);
        when(properties.getPublisher().isEnabled()).thenReturn(true);
        when(properties.getConsumer().isEnabled()).thenReturn(false);
        when(properties.toStringWithPublisher()).thenReturn("publisher-props");

        CompositePostApplicationReadyOutboxInitializer composite =
                new CompositePostApplicationReadyOutboxInitializer(properties, Collections.emptyList());

        composite.init();

        verify(properties, times(1)).toStringWithPublisher();
        verify(properties, never()).toStringWithConsumer();
    }

    @Test
    @DisplayName("UT init() when ONLY consumer is enabled should log consumer properties")
    void init_whenOnlyConsumerEnabled_shouldLogConsumerProperties() {
        OutboxProperties properties = mock(OutboxProperties.class, RETURNS_DEEP_STUBS);
        when(properties.getPublisher().isEnabled()).thenReturn(false);
        when(properties.getConsumer().isEnabled()).thenReturn(true);
        when(properties.toStringWithConsumer()).thenReturn("consumer-props");

        CompositePostApplicationReadyOutboxInitializer composite =
                new CompositePostApplicationReadyOutboxInitializer(properties, Collections.emptyList());

        composite.init();

        verify(properties, never()).toStringWithPublisher();
        verify(properties, times(1)).toStringWithConsumer();
    }

    @Test
    @DisplayName("UT init() when BOTH are enabled should log base properties")
    void init_whenBothEnabled_shouldLogBaseProperties() {
        OutboxProperties properties = mock(OutboxProperties.class, RETURNS_DEEP_STUBS);
        when(properties.getPublisher().isEnabled()).thenReturn(true);
        when(properties.getConsumer().isEnabled()).thenReturn(true);

        CompositePostApplicationReadyOutboxInitializer composite =
                new CompositePostApplicationReadyOutboxInitializer(properties, Collections.emptyList());

        composite.init();

        verify(properties, never()).toStringWithPublisher();
        verify(properties, never()).toStringWithConsumer();
    }

    @Test
    @DisplayName("UT init() when BOTH are disabled should log base properties")
    void init_whenBothDisabled_shouldLogBaseProperties() {
        OutboxProperties properties = mock(OutboxProperties.class, RETURNS_DEEP_STUBS);
        when(properties.getPublisher().isEnabled()).thenReturn(false);
        when(properties.getConsumer().isEnabled()).thenReturn(false);

        CompositePostApplicationReadyOutboxInitializer composite =
                new CompositePostApplicationReadyOutboxInitializer(properties, Collections.emptyList());

        composite.init();

        verify(properties, never()).toStringWithPublisher();
        verify(properties, never()).toStringWithConsumer();
    }
}