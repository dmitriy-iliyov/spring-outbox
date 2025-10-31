//package io.github.dmitriyiliyov.springoutbox.unit.config;
//
//import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import java.time.Duration;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//
//public class OutboxPropertiesDefaultsUnitTests {
//
//    @Test
//    @DisplayName("UT Defaults() when all parameters are null or invalid should assign default values")
//    public void defaultsConstructor_whenParametersNullOrInvalid_shouldAssignDefaults() {
//        // given
//        Integer batchSize = null;
//        Duration initialDelay = null;
//        Duration fixedDelay = null;
//        Integer maxRetries = null;
//        OutboxProperties.BackoffProperties backoff = null;
//
//        // when
//        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
//                batchSize,
//                initialDelay,
//                fixedDelay,
//                maxRetries,
//                backoff
//        );
//
//        // then
//        assertEquals(50, defaults.getBatchSize());
//        assertEquals(Duration.ofSeconds(300), defaults.getInitialDelay());
//        assertEquals(Duration.ofSeconds(2), defaults.getFixedDelay());
//        assertEquals(3, defaults.getMaxRetries());
//        assertNotNull(defaults.getBackoff());
//    }
//
//    @Test
//    @DisplayName("UT Defaults() when batchSize <= 0 should assign default batch size")
//    public void defaultsConstructor_whenBatchSizeLessOrEqualZero_shouldAssignDefaultBatchSize() {
//        // given
//        int batchSize = 0;
//
//        // when
//        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
//                batchSize,
//                Duration.ofSeconds(1),
//                Duration.ofSeconds(1),
//                1,
//                new OutboxProperties.BackoffProperties()
//        );
//
//        // then
//        assertEquals(50, defaults.getBatchSize());
//    }
//
//    @Test
//    @DisplayName("UT Defaults() when maxRetries < 0 should assign default maxRetries")
//    public void defaultsConstructor_whenMaxRetriesLessThanZero_shouldAssignDefaultMaxRetries() {
//        // given
//        int maxRetries = -1;
//
//        // when
//        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
//                10,
//                Duration.ofSeconds(1),
//                Duration.ofSeconds(1),
//                maxRetries,
//                new OutboxProperties.BackoffProperties()
//        );
//
//        // then
//        assertEquals(3, defaults.getMaxRetries());
//    }
//
//    @Test
//    @DisplayName("UT Defaults() when provided values are valid should use provided ones")
//    public void defaultsConstructor_whenValidValuesProvided_shouldUseThem() {
//        // given
//        OutboxProperties.BackoffProperties customBackoff = new OutboxProperties.BackoffProperties();
//
//        // when
//        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
//                100, Duration.ofSeconds(10), Duration.ofSeconds(5), 7, customBackoff);
//
//        // then
//        assertEquals(100, defaults.getBatchSize());
//        assertEquals(Duration.ofSeconds(10), defaults.getInitialDelay());
//        assertEquals(Duration.ofSeconds(5), defaults.getFixedDelay());
//        assertEquals(7, defaults.getMaxRetries());
//        assertEquals(customBackoff, defaults.getBackoff());
//    }
//
//    @Test
//    @DisplayName("UT Defaults() when backoff is null should assign default backoff")
//    public void defaultsConstructor_whenBackoffNull_shouldAssignDefaultBackoff() {
//        // given
//        OutboxProperties.BackoffProperties backoff = null;
//
//        // when
//        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
//                10,
//                Duration.ofSeconds(1),
//                Duration.ofSeconds(1),
//                2,
//                backoff
//        );
//
//        // then
//        assertNotNull(defaults.getBackoff());
//    }
//
//    @Test
//    @DisplayName("UT Defaults() when initialDelay and fixedDelay are null should assign defaults")
//    public void defaultsConstructor_whenDelaysAreNull_shouldAssignDefaults() {
//        // given
//        Duration initialDelay = null;
//        Duration fixedDelay = null;
//
//        // when
//        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
//                10,
//                initialDelay,
//                fixedDelay,
//                2,
//                new OutboxProperties.BackoffProperties()
//        );
//
//        // then
//        assertEquals(Duration.ofSeconds(300), defaults.getInitialDelay());
//        assertEquals(Duration.ofSeconds(2), defaults.getFixedDelay());
//    }
//}
