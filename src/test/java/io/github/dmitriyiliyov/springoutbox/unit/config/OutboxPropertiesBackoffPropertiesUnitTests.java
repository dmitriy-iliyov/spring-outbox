package io.github.dmitriyiliyov.springoutbox.unit.config;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties.BackoffProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class OutboxPropertiesBackoffPropertiesUnitTests {

    @Test
    @DisplayName("UT default constructor should enable backoff with default values")
    public void constructor_default_shouldEnableWithDefaults() {
        // given + when
        BackoffProperties backoff = new BackoffProperties();

        // then
        assertTrue(backoff.isEnabled());
        assertEquals(Duration.ofSeconds(10), backoff.getDelay());
        assertEquals(3L, backoff.getMultiplier());
    }

    @Test
    @DisplayName("UT constructor with enabled = true and null values should use defaults")
    public void constructor_enabledTrueWithNullValues_shouldUseDefaults() {
        // given + when
        BackoffProperties backoff = new BackoffProperties(true, null, null);

        // then
        assertTrue(backoff.isEnabled());
        assertEquals(Duration.ofSeconds(10), backoff.getDelay());
        assertEquals(3L, backoff.getMultiplier());
    }

    @Test
    @DisplayName("UT constructor with enabled = false should disable backoff and set minimal values")
    public void constructor_enabledFalse_shouldDisableBackoff() {
        // given + when
        BackoffProperties backoff = new BackoffProperties(false, Duration.ofSeconds(100), 10L);

        // then
        assertFalse(backoff.isEnabled());
        assertEquals(Duration.ofSeconds(0), backoff.getDelay());
        assertEquals(1L, backoff.getMultiplier());
    }

    @Test
    @DisplayName("UT constructor with enabled = null should treat as enabled and use defaults")
    public void constructor_enabledNull_shouldEnableWithDefaults() {
        // given + when
        BackoffProperties backoff = new BackoffProperties(null, null, null);

        // then
        assertTrue(backoff.isEnabled());
        assertEquals(Duration.ofSeconds(10), backoff.getDelay());
        assertEquals(3L, backoff.getMultiplier());
    }

    @Test
    @DisplayName("UT constructor with valid delay and multiplier should assign provided values")
    public void constructor_validDelayAndMultiplier_shouldAssignValues() {
        // given
        Duration customDelay = Duration.ofSeconds(20);
        Long customMultiplier = 5L;

        // when
        BackoffProperties backoff = new BackoffProperties(true, customDelay, customMultiplier);

        // then
        assertTrue(backoff.isEnabled());
        assertEquals(customDelay, backoff.getDelay());
        assertEquals(customMultiplier, backoff.getMultiplier());
    }

    @Test
    @DisplayName("UT constructor with multiplier < 1 should assign default multiplier")
    public void constructor_multiplierLessThanOne_shouldUseDefaultMultiplier() {
        // given
        Long invalidMultiplier = 0L;

        // when
        BackoffProperties backoff = new BackoffProperties(true, Duration.ofSeconds(5), invalidMultiplier);

        // then
        assertTrue(backoff.isEnabled());
        assertEquals(Duration.ofSeconds(5), backoff.getDelay());
        assertEquals(3L, backoff.getMultiplier());
    }

    @Test
    @DisplayName("UT equals and hashCode should work correctly")
    public void equalsAndHashCode_shouldCompareCorrectly() {
        // given
        BackoffProperties backoff1 = new BackoffProperties(true, Duration.ofSeconds(10), 3L);
        BackoffProperties backoff2 = new BackoffProperties(true, Duration.ofSeconds(10), 3L);
        BackoffProperties backoff3 = new BackoffProperties(false, Duration.ofSeconds(0), 1L);

        // then
        assertEquals(backoff1, backoff2);
        assertEquals(backoff1.hashCode(), backoff2.hashCode());
        assertNotEquals(backoff1, backoff3);
        assertNotEquals(backoff1.hashCode(), backoff3.hashCode());
    }
}
