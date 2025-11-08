package io.github.dmitriyiliyov.springoutbox.unit.config;

import io.github.dmitriyiliyov.springoutbox.config.CleanUpProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class OutboxPropertiesCleanUpPublisherPropertiesUnitTests {

    @Test
    @DisplayName("UT initialize() with enabled = true and no parameters should use defaults")
    public void initialize_enabledTrue_noParams_shouldUseDefaults() {
        // given
        CleanUpProperties cleanup = new CleanUpProperties();
        cleanup.setEnabled(true);

        // when
        cleanup.initialize();

        // then
        Assertions.assertTrue(cleanup.isEnabled());
        assertEquals(100, cleanup.getBatchSize());
        assertEquals(Duration.ofHours(1), cleanup.getTtl());
        assertEquals(Duration.ofSeconds(300), cleanup.getInitialDelay());
        assertEquals(Duration.ofSeconds(5), cleanup.getFixedDelay());
    }

    @Test
    @DisplayName("UT initialize() with enabled = true and valid parameters should assign values")
    public void initialize_enabledTrue_withValues_shouldAssignValues() {
        // given
        CleanUpProperties cleanup = new CleanUpProperties();
        cleanup.setEnabled(true);
        cleanup.setBatchSize(50);
        cleanup.setTtl(Duration.ofMinutes(30));
        cleanup.setInitialDelay(Duration.ofSeconds(100));
        cleanup.setFixedDelay(Duration.ofSeconds(10));

        // when
        cleanup.initialize();

        // then
        assertTrue(cleanup.isEnabled());
        assertEquals(50, cleanup.getBatchSize());
        assertEquals(Duration.ofMinutes(30), cleanup.getTtl());
        assertEquals(Duration.ofSeconds(100), cleanup.getInitialDelay());
        assertEquals(Duration.ofSeconds(10), cleanup.getFixedDelay());
    }

    @Test
    @DisplayName("UT initialize() with enabled = true and some null/invalid fields should fallback to defaults")
    public void initialize_enabledTrue_partialNull_shouldUseDefaultsForNull() {
        // given
        CleanUpProperties cleanup = new CleanUpProperties();
        cleanup.setEnabled(true);
        cleanup.setBatchSize(-1);
        cleanup.setTtl(null);
        cleanup.setInitialDelay(Duration.ofSeconds(200));
        cleanup.setFixedDelay(null);

        // when
        cleanup.initialize();

        // then
        assertTrue(cleanup.isEnabled());
        assertEquals(100, cleanup.getBatchSize());
        assertEquals(Duration.ofHours(1), cleanup.getTtl());
        assertEquals(Duration.ofSeconds(200), cleanup.getInitialDelay());
        assertEquals(Duration.ofSeconds(5), cleanup.getFixedDelay());
    }

    @Test
    @DisplayName("UT initialize() with enabled = false should disable all fields")
    public void initialize_enabledFalse_shouldDisableAndIgnoreValues() {
        // given
        CleanUpProperties cleanup = new CleanUpProperties();
        cleanup.setEnabled(false);
        cleanup.setBatchSize(50);
        cleanup.setTtl(Duration.ofMinutes(30));
        cleanup.setInitialDelay(Duration.ofSeconds(100));
        cleanup.setFixedDelay(Duration.ofSeconds(10));

        // when
        cleanup.initialize();

        // then
        assertFalse(cleanup.isEnabled());
        assertEquals(0, cleanup.getBatchSize());
        assertNull(cleanup.getTtl());
        assertNull(cleanup.getInitialDelay());
        assertNull(cleanup.getFixedDelay());
    }

    @Test
    @DisplayName("UT initialize() with enabled = null should treat as false and disable fields")
    public void initialize_enabledNull_shouldDisable() {
        // given
        CleanUpProperties cleanup = new CleanUpProperties();
        cleanup.setEnabled(null);

        // when
        cleanup.initialize();

        // then
        assertFalse(cleanup.isEnabled());
        assertEquals(0, cleanup.getBatchSize());
        assertNull(cleanup.getTtl());
        assertNull(cleanup.getInitialDelay());
        assertNull(cleanup.getFixedDelay());
    }
}
