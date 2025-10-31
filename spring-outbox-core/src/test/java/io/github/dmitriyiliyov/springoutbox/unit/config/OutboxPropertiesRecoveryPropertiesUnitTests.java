//package io.github.dmitriyiliyov.springoutbox.unit.config;
//
//import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties.RecoveryProperties;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import java.time.Duration;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotEquals;
//
//public class OutboxPropertiesRecoveryPropertiesUnitTests {
//
//    @Test
//    @DisplayName("UT default constructor should assign default values")
//    public void constructor_default_shouldAssignDefaults() {
//        // given + when
//        RecoveryProperties recovery = new RecoveryProperties();
//
//        // then
//        assertEquals(100, recovery.getBatchSize());
//        assertEquals(Duration.ofSeconds(300), recovery.getInitialDelay());
//        assertEquals(Duration.ofSeconds(1800), recovery.getFixedDelay());
//    }
//
//    @Test
//    @DisplayName("UT constructor with null parameters should assign default values")
//    public void constructor_nullParameters_shouldAssignDefaults() {
//        // given + when
//        RecoveryProperties recovery = new RecoveryProperties(null, null, null);
//
//        // then
//        assertEquals(100, recovery.getBatchSize());
//        assertEquals(Duration.ofSeconds(300), recovery.getInitialDelay());
//        assertEquals(Duration.ofSeconds(1800), recovery.getFixedDelay());
//    }
//
//    @Test
//    @DisplayName("UT constructor with negative batchSize should assign default batchSize")
//    public void constructor_negativeBatchSize_shouldUseDefault() {
//        // given + when
//        RecoveryProperties recovery = new RecoveryProperties(-10, Duration.ofSeconds(100), Duration.ofSeconds(200));
//
//        // then
//        assertEquals(100, recovery.getBatchSize());
//        assertEquals(Duration.ofSeconds(100), recovery.getInitialDelay());
//        assertEquals(Duration.ofSeconds(200), recovery.getFixedDelay());
//    }
//
//    @Test
//    @DisplayName("UT constructor with valid parameters should assign provided values")
//    public void constructor_validParameters_shouldAssignValues() {
//        // given
//        int batchSize = 50;
//        Duration initialDelay = Duration.ofSeconds(60);
//        Duration fixedDelay = Duration.ofSeconds(120);
//
//        // when
//        RecoveryProperties recovery = new RecoveryProperties(batchSize, initialDelay, fixedDelay);
//
//        // then
//        assertEquals(batchSize, recovery.getBatchSize());
//        assertEquals(initialDelay, recovery.getInitialDelay());
//        assertEquals(fixedDelay, recovery.getFixedDelay());
//    }
//
//    @Test
//    @DisplayName("UT equals and hashCode should work correctly")
//    public void equalsAndHashCode_shouldCompareCorrectly() {
//        // given
//        RecoveryProperties recovery1 = new RecoveryProperties();
//        RecoveryProperties recovery2 = new RecoveryProperties();
//        RecoveryProperties recovery3 = new RecoveryProperties(50, Duration.ofSeconds(60), Duration.ofSeconds(120));
//
//        // then
//        assertEquals(recovery1, recovery2);
//        assertEquals(recovery1.hashCode(), recovery2.hashCode());
//        assertNotEquals(recovery1, recovery3);
//        assertNotEquals(recovery1.hashCode(), recovery3.hashCode());
//    }
//}
