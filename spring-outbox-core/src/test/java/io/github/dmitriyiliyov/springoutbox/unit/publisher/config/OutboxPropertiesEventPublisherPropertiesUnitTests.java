package io.github.dmitriyiliyov.springoutbox.unit.publisher.config;

import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherProperties.BackoffProperties;
import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherProperties.Defaults;
import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherProperties.EventProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OutboxPropertiesEventPublisherPropertiesUnitTests {

    @Test
    @DisplayName("UT initialize() should throw when eventType is null")
    public void afterPropertiesSet_eventTypeNull_shouldThrow() {
        // given
        EventProperties event = new EventProperties();
        event.setEventType(null);
        event.setTopic("topic");
        Defaults defaults = new Defaults();
        defaults.afterPropertiesSet();

        // when + then
        assertThrows(IllegalArgumentException.class,
                () -> event.afterPropertiesSet(defaults));
    }

    @Test
    @DisplayName("UT initialize() should throw when eventType is blank")
    public void afterPropertiesSet_eventTypeBlank_shouldThrow() {
        // given
        EventProperties event = new EventProperties();
        event.setEventType("   ");
        event.setTopic("topic");
        Defaults defaults = new Defaults();
        defaults.afterPropertiesSet();

        // when + then
        assertThrows(IllegalArgumentException.class,
                () -> event.afterPropertiesSet(defaults));
    }

    @Test
    @DisplayName("UT initialize() should throw when topic is null")
    public void afterPropertiesSet_topicNull_shouldThrow() {
        // given
        EventProperties event = new EventProperties();
        event.setEventType("event");
        event.setTopic(null);
        Defaults defaults = new Defaults();
        defaults.afterPropertiesSet();

        // when + then
        assertThrows(IllegalArgumentException.class,
                () -> event.afterPropertiesSet(defaults));
    }

    @Test
    @DisplayName("UT initialize() should throw when topic is blank")
    public void afterPropertiesSet_topicBlank_shouldThrow() {
        // given
        EventProperties event = new EventProperties();
        event.setEventType("event");
        event.setTopic("   ");
        Defaults defaults = new Defaults();
        defaults.afterPropertiesSet();

        // when + then
        assertThrows(IllegalArgumentException.class,
                () -> event.afterPropertiesSet(defaults));
    }

    @Test
    @DisplayName("UT initialize() should assign default values when fields are null")
    public void afterPropertiesSet_nullFields_shouldUseDefaults() {
        // given
        EventProperties event = new EventProperties();
        event.setEventType("event");
        event.setTopic("topic");
        event.setBatchSize(null);
        event.setInitialDelay(null);
        event.setFixedDelay(null);
        event.setMaxRetries(null);
        event.setBackoff(null);
        Defaults defaults = new Defaults();
        defaults.afterPropertiesSet();

        // when
        event.afterPropertiesSet(defaults);

        // then
        assertEquals(defaults.getBatchSize(), event.getBatchSize());
        assertEquals(defaults.getInitialDelay(), event.getInitialDelay());
        assertEquals(defaults.getFixedDelay(), event.getFixedDelay());
        assertEquals(defaults.getMaxRetries(), event.getMaxRetries());
        assertEquals(defaults.getBackoff(), event.getBackoff());
    }

    @Test
    @DisplayName("UT backoffMultiplier() should return correct multiplier")
    public void backoffMultiplier_shouldReturnCorrectValue() {
        // given
        BackoffProperties backoff = new BackoffProperties(true, Duration.ofSeconds(5), 7L);
        EventProperties event = new EventProperties();
        event.setEventType("event");
        event.setTopic("topic");
        event.setBackoff(backoff);
        Defaults defaults = new Defaults();
        defaults.afterPropertiesSet();

        // when
        event.afterPropertiesSet(defaults);
        long multiplier = event.backoffMultiplier();

        // then
        assertEquals(7L, multiplier);
    }

    @Test
    @DisplayName("UT backoffDelay() should return correct delay in seconds")
    public void backoffDelay_shouldReturnCorrectValue() {
        // given
        BackoffProperties backoff = new BackoffProperties(true, Duration.ofSeconds(15), 3L);
        EventProperties event = new EventProperties();
        event.setEventType("event");
        event.setTopic("topic");
        event.setBackoff(backoff);
        Defaults defaults = new Defaults();
        defaults.afterPropertiesSet();

        // when
        event.afterPropertiesSet(defaults);
        long delay = event.backoffDelay();

        // then
        assertEquals(15L, delay);
    }

    @Test
    @DisplayName("UT initialize() with valid parameters should create object")
    public void afterPropertiesSet_validParameters_shouldCreateObject() {
        // given
        BackoffProperties backoff = new BackoffProperties(true, Duration.ofSeconds(10), 5L);
        EventProperties event = new EventProperties();
        event.setEventType("user-registered");
        event.setTopic("user-topic");
        event.setBatchSize(20);
        event.setInitialDelay(Duration.ofSeconds(5));
        event.setFixedDelay(Duration.ofSeconds(10));
        event.setMaxRetries(3);
        event.setBackoff(backoff);
        Defaults defaults = new Defaults();
        defaults.afterPropertiesSet();

        // when
        event.afterPropertiesSet(defaults);

        // then
        assertEquals("user-registered", event.getEventType());
        assertEquals("user-topic", event.getTopic());
        assertEquals(20, event.getBatchSize());
        assertEquals(Duration.ofSeconds(5), event.getInitialDelay());
        assertEquals(Duration.ofSeconds(10), event.getFixedDelay());
        assertEquals(3, event.getMaxRetries());
        assertEquals(backoff, event.getBackoff());
    }
}
