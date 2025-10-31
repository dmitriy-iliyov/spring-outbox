//package io.github.dmitriyiliyov.springoutbox.unit.config;
//
//import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties.DlqProperties;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import java.time.Duration;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//public class OutboxPropertiesDlqPropertiesUnitTests {
//
//    @Test
//    @DisplayName("UT default enabled with null parameters should use defaults")
//    public void constructor_enabledNullParameters_shouldUseDefaults() {
//        // given + when
//        DlqProperties dlq = new DlqProperties(true, null, null,
//                null, null, null);
//
//        // then
//        assertTrue(dlq.enabled());
//        assertEquals(100, dlq.batchSize());
//        assertEquals(Duration.ofSeconds(300), dlq.transferToDlqInitialDelay());
//        assertEquals(Duration.ofSeconds(900), dlq.transferToDlqFixedDelay());
//        assertEquals(Duration.ofSeconds(300), dlq.transferFromDlqInitialDelay());
//        assertEquals(Duration.ofSeconds(3600), dlq.transferFormDlqFixedDelay());
//    }
//
//    @Test
//    @DisplayName("UT enabled = true with valid parameters should assign values")
//    public void constructor_enabledTrueWithValues_shouldAssignValues() {
//        // given
//        int batchSize = 50;
//        Duration toInitial = Duration.ofSeconds(100);
//        Duration toFixed = Duration.ofSeconds(200);
//        Duration fromInitial = Duration.ofSeconds(150);
//        Duration fromFixed = Duration.ofSeconds(300);
//
//        // when
//        DlqProperties dlq = new DlqProperties(true, batchSize, toInitial, toFixed, fromInitial, fromFixed);
//
//        // then
//        assertTrue(dlq.enabled());
//        assertEquals(batchSize, dlq.batchSize());
//        assertEquals(toInitial, dlq.transferToDlqInitialDelay());
//        assertEquals(toFixed, dlq.transferToDlqFixedDelay());
//        assertEquals(fromInitial, dlq.transferFromDlqInitialDelay());
//        assertEquals(fromFixed, dlq.transferFormDlqFixedDelay());
//    }
//
//    @Test
//    @DisplayName("UT enabled = true with some null/invalid parameters should use defaults for those")
//    public void constructor_enabledTruePartialNullOrInvalid_shouldUseDefaults() {
//        // given + when
//        DlqProperties dlq = new DlqProperties(true, -1, null, Duration.ofSeconds(50),
//                null, null);
//
//        // then
//        assertTrue(dlq.enabled());
//        assertEquals(100, dlq.batchSize());
//        assertEquals(Duration.ofSeconds(300), dlq.transferToDlqInitialDelay());
//        assertEquals(Duration.ofSeconds(50), dlq.transferToDlqFixedDelay());
//        assertEquals(Duration.ofSeconds(300), dlq.transferFromDlqInitialDelay());
//        assertEquals(Duration.ofSeconds(3600), dlq.transferFormDlqFixedDelay());
//    }
//
//    @Test
//    @DisplayName("UT enabled = false should disable DLQ and set null")
//    public void constructor_enabledFalse_shouldDisableAndIgnoreValues() {
//        // given + when
//        DlqProperties dlq = new DlqProperties(false, 50, Duration.ofSeconds(10),
//                Duration.ofSeconds(20), Duration.ofSeconds(30), Duration.ofSeconds(40));
//
//        // then
//        assertFalse(dlq.enabled());
//        assertEquals(0, dlq.batchSize());
//        assertNull(dlq.transferToDlqInitialDelay());
//        assertNull(dlq.transferToDlqFixedDelay());
//        assertNull(dlq.transferFromDlqInitialDelay());
//        assertNull(dlq.transferFormDlqFixedDelay());
//    }
//
//    @Test
//    @DisplayName("UT enabled = null should treat as disable DLQ and set null")
//    public void constructor_enabledNull_shouldTreatAsEnabled() {
//        // given + when
//        DlqProperties dlq = new DlqProperties(null, null, null,
//                null, null, null);
//
//        // then
//        assertFalse(dlq.enabled());
//        assertEquals(0, dlq.batchSize());
//        assertNull(dlq.transferToDlqInitialDelay());
//        assertNull(dlq.transferToDlqFixedDelay());
//        assertNull(dlq.transferFromDlqInitialDelay());
//        assertNull(dlq.transferFormDlqFixedDelay());
//    }
//}
