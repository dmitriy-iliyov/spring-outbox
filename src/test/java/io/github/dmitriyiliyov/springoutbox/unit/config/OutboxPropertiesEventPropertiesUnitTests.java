package io.github.dmitriyiliyov.springoutbox.unit.config;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties.BackoffProperties;
import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties.EventProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OutboxPropertiesEventPropertiesUnitTests {

    @Test
    @DisplayName("UT constructor should throw when eventType is null")
    public void constructor_whenEventTypeNull_thenThrow() {
        // given + when + then
        assertThrows(NullPointerException.class,
                () -> new EventProperties(null, "topic", 1,
                        Duration.ofSeconds(1), Duration.ofSeconds(1), 1, new BackoffProperties())
        );
    }

    @Test
    @DisplayName("UT constructor should throw when eventType is blank")
    public void constructor_whenEventTypeBlank_thenThrow() {
        // given + when + then
        assertThrows(IllegalArgumentException.class,
                () -> new EventProperties("   ", "topic", 1,
                        Duration.ofSeconds(1), Duration.ofSeconds(1), 1, new BackoffProperties())
        );
    }

    @Test
    @DisplayName("UT constructor should throw when topic is null")
    public void constructor_whenTopicNull_thenThrow() {
        // given + when + then
        assertThrows(NullPointerException.class,
                () -> new EventProperties("event", null, 1,
                        Duration.ofSeconds(1), Duration.ofSeconds(1), 1, new BackoffProperties())
        );
    }

    @Test
    @DisplayName("UT constructor should throw when topic is blank")
    public void constructor_whenTopicBlank_thenThrow() {
        // given + when + then
        assertThrows(IllegalArgumentException.class,
                () -> new EventProperties("event", "   ", 1,
                        Duration.ofSeconds(1), Duration.ofSeconds(1), 1, new BackoffProperties())
        );
    }

    @Test
    @DisplayName("UT backoffMultiplier() should return correct multiplier")
    public void backoffMultiplier_shouldReturnCorrectValue() {
        // given
        BackoffProperties backoff = new BackoffProperties(true, Duration.ofSeconds(5), 7L);
        EventProperties event = new EventProperties("event", "topic", 10,
                Duration.ofSeconds(1), Duration.ofSeconds(2), 3, backoff);

        // when
        long multiplier = event.backoffMultiplier();

        // then
        assertEquals(7L, multiplier);
    }

    @Test
    @DisplayName("UT backoffDelay() should return correct delay in seconds")
    public void backoffDelay_shouldReturnCorrectValue() {
        // given
        BackoffProperties backoff = new BackoffProperties(true, Duration.ofSeconds(15), 3L);
        EventProperties event = new EventProperties("event", "topic", 10,
                Duration.ofSeconds(1), Duration.ofSeconds(2), 3, backoff);

        // when
        long delay = event.backoffDelay();

        // then
        assertEquals(15L, delay);
    }

    @Test
    @DisplayName("UT constructor with valid parameters should create object")
    public void constructor_withValidParameters_shouldCreateObject() {
        // given
        BackoffProperties backoff = new BackoffProperties(true, Duration.ofSeconds(10), 5L);

        // when
        EventProperties event = new EventProperties("user-registered", "user-topic", 20,
                Duration.ofSeconds(5), Duration.ofSeconds(10), 3, backoff);

        // then
        assertEquals("user-registered", event.eventType());
        assertEquals("user-topic", event.topic());
        assertEquals(20, event.batchSize());
        assertEquals(Duration.ofSeconds(5), event.initialDelay());
        assertEquals(Duration.ofSeconds(10), event.fixedDelay());
        assertEquals(3, event.maxRetries());
        assertEquals(backoff, event.backoff());
    }
}
