package io.github.dmitriyiliyov.springoutbox.unit.config;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties.BackoffProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class OutboxPropertiesBackoffPropertiesUnitTests {

    @Test
    @DisplayName("UT initialize() with default constructor should assign default values")
    public void initialize_defaultConstructor_assignsDefaults() {
        // given
        BackoffProperties backoff = new BackoffProperties();

        // when
        backoff.initialize();

        // then
        assertTrue(backoff.isEnabled());
        assertEquals(Duration.ofSeconds(10), backoff.getDelay());
        assertEquals(3L, backoff.getMultiplier());
    }

    @Test
    @DisplayName("UT initialize() with enabled=true and null fields should assign defaults")
    public void initialize_enabledTrueWithNull_assignsDefaults() {
        // given
        BackoffProperties backoff = new BackoffProperties();
        backoff.setEnabled(true);
        backoff.setDelay(null);
        backoff.setMultiplier(null);

        // when
        backoff.initialize();

        // then
        assertTrue(backoff.isEnabled());
        assertEquals(Duration.ofSeconds(10), backoff.getDelay());
        assertEquals(3L, backoff.getMultiplier());
    }

    @Test
    @DisplayName("UT initialize() with enabled=false should disable backoff and set minimal values")
    public void initialize_enabledFalse_disablesBackoff() {
        // given
        BackoffProperties backoff = new BackoffProperties();
        backoff.setEnabled(false);
        backoff.setDelay(Duration.ofSeconds(100));
        backoff.setMultiplier(10L);

        // when
        backoff.initialize();

        // then
        assertFalse(backoff.isEnabled());
        assertEquals(Duration.ofSeconds(0), backoff.getDelay());
        assertEquals(1L, backoff.getMultiplier());
    }

    @Test
    @DisplayName("UT initialize() with enabled=null should treat as enabled and assign defaults")
    public void initialize_enabledNull_assignsDefaults() {
        // given
        BackoffProperties backoff = new BackoffProperties();
        backoff.setEnabled(null);
        backoff.setDelay(null);
        backoff.setMultiplier(null);

        // when
        backoff.initialize();

        // then
        assertTrue(backoff.isEnabled());
        assertEquals(Duration.ofSeconds(10), backoff.getDelay());
        assertEquals(3L, backoff.getMultiplier());
    }

    @Test
    @DisplayName("UT initialize() with valid delay and multiplier should keep assigned values")
    public void initialize_validDelayAndMultiplier_keepsValues() {
        // given
        BackoffProperties backoff = new BackoffProperties();
        backoff.setEnabled(true);
        backoff.setDelay(Duration.ofSeconds(20));
        backoff.setMultiplier(5L);

        // when
        backoff.initialize();

        // then
        assertTrue(backoff.isEnabled());
        assertEquals(Duration.ofSeconds(20), backoff.getDelay());
        assertEquals(5L, backoff.getMultiplier());
    }

    @Test
    @DisplayName("UT initialize() with multiplier < 1 should assign default multiplier")
    public void initialize_multiplierLessThanOne_usesDefaultMultiplier() {
        // given
        BackoffProperties backoff = new BackoffProperties();
        backoff.setEnabled(true);
        backoff.setDelay(Duration.ofSeconds(5));
        backoff.setMultiplier(0L);

        // when
        backoff.initialize();

        // then
        assertTrue(backoff.isEnabled());
        assertEquals(Duration.ofSeconds(5), backoff.getDelay());
        assertEquals(3L, backoff.getMultiplier());
    }

    @Test
    @DisplayName("UT equals() and hashCode() should work correctly for identical and different objects")
    public void equalsAndHashCode_correctBehavior() {
        // given
        BackoffProperties b1 = new BackoffProperties();
        b1.setEnabled(true);
        b1.setDelay(Duration.ofSeconds(10));
        b1.setMultiplier(3L);
        b1.initialize();

        BackoffProperties b2 = new BackoffProperties();
        b2.setEnabled(true);
        b2.setDelay(Duration.ofSeconds(10));
        b2.setMultiplier(3L);
        b2.initialize();

        BackoffProperties b3 = new BackoffProperties();
        b3.setEnabled(false);
        b3.setDelay(Duration.ofSeconds(0));
        b3.setMultiplier(1L);
        b3.initialize();

        // then
        assertEquals(b1, b2);
        assertEquals(b1.hashCode(), b2.hashCode());
        assertNotEquals(b1, b3);
        assertNotEquals(b1.hashCode(), b3.hashCode());
    }
}
