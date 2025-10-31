//package io.github.dmitriyiliyov.springoutbox.unit.config;
//
//import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties.CleanUpProperties;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import java.time.Duration;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//public class OutboxPropertiesCleanUpPropertiesUnitTests {
//
//    @Test
//    @DisplayName("UT default enabled with null parameters should use defaults")
//    public void constructor_enabledNullParameters_shouldUseDefaults() {
//        // given + when
//        CleanUpProperties cleanup = new CleanUpProperties(true, null, null, null, null);
//
//        // then
//        assertTrue(cleanup.enabled());
//        assertEquals(100, cleanup.batchSize());
//        assertEquals(Duration.ofHours(1), cleanup.threshold());
//        assertEquals(Duration.ofSeconds(300), cleanup.initialDelay());
//        assertEquals(Duration.ofSeconds(5), cleanup.fixedDelay());
//    }
//
//    @Test
//    @DisplayName("UT enabled = true with valid parameters should assign values")
//    public void constructor_enabledTrueWithValues_shouldAssignValues() {
//        // given
//        int batchSize = 50;
//        Duration threshold = Duration.ofMinutes(30);
//        Duration initialDelay = Duration.ofSeconds(100);
//        Duration fixedDelay = Duration.ofSeconds(10);
//
//        // when
//        CleanUpProperties cleanup = new CleanUpProperties(true, batchSize, threshold, initialDelay, fixedDelay);
//
//        // then
//        assertTrue(cleanup.enabled());
//        assertEquals(batchSize, cleanup.batchSize());
//        assertEquals(threshold, cleanup.threshold());
//        assertEquals(initialDelay, cleanup.initialDelay());
//        assertEquals(fixedDelay, cleanup.fixedDelay());
//    }
//
//    @Test
//    @DisplayName("UT enabled = true with some null/invalid parameters should use defaults for those")
//    public void constructor_enabledTrueWithPartialNull_shouldUseDefaultsForNull() {
//        // given + when
//        CleanUpProperties cleanup = new CleanUpProperties(true, -1, null, Duration.ofSeconds(200), null);
//
//        // then
//        assertTrue(cleanup.enabled());
//        assertEquals(100, cleanup.batchSize());
//        assertEquals(Duration.ofHours(1), cleanup.threshold());
//        assertEquals(Duration.ofSeconds(200), cleanup.initialDelay());
//        assertEquals(Duration.ofSeconds(5), cleanup.fixedDelay());
//    }
//
//    @Test
//    @DisplayName("UT enabled = false with parameters should ignore values and set disabled defaults")
//    public void constructor_enabledFalse_shouldDisableAndIgnoreValues() {
//        // given + when
//        CleanUpProperties cleanup = new CleanUpProperties(false, 50, Duration.ofMinutes(30), Duration.ofSeconds(100), Duration.ofSeconds(10));
//
//        // then
//        assertFalse(cleanup.enabled());
//        assertEquals(0, cleanup.batchSize());
//        assertNull(cleanup.threshold());
//        assertNull(cleanup.initialDelay());
//        assertNull(cleanup.fixedDelay());
//    }
//
//    @Test
//    @DisplayName("UT enabled = null should treat as enabled and assign defaults")
//    public void constructor_enabledNull_shouldTreatAsEnabled() {
//        // given + when
//        CleanUpProperties cleanup = new CleanUpProperties(null, null, null, null, null);
//
//        // then
//        assertFalse(cleanup.enabled());
//        assertEquals(0, cleanup.batchSize());
//        assertNull(cleanup.threshold());
//        assertNull(cleanup.initialDelay());
//        assertNull(cleanup.fixedDelay());
//    }
//}
