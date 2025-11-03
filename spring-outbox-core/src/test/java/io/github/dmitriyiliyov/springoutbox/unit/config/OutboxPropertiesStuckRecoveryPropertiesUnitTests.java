package io.github.dmitriyiliyov.springoutbox.unit.config;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class OutboxPropertiesStuckRecoveryPropertiesUnitTests {

    @Test
    @DisplayName("UT initialize() with no parameters should assign default values")
    public void initialize_noParams_shouldAssignDefaults() {
        // given + when
        OutboxProperties.StuckRecoveryProperties recovery = new OutboxProperties.StuckRecoveryProperties();
        recovery.initialize();

        // then
        assertEquals(100, recovery.getBatchSize());
        assertEquals(Duration.ofSeconds(300), recovery.getInitialDelay());
        assertEquals(Duration.ofSeconds(1800), recovery.getFixedDelay());
    }

    @Test
    @DisplayName("UT initialize() with null parameters should assign default values")
    public void initialize_nullParameters_shouldAssignDefaults() {
        // given
        OutboxProperties.StuckRecoveryProperties recovery = new OutboxProperties.StuckRecoveryProperties();
        recovery.setBatchSize(null);
        recovery.setInitialDelay(null);
        recovery.setFixedDelay(null);

        // when
        recovery.initialize();

        // then
        assertEquals(100, recovery.getBatchSize());
        assertEquals(Duration.ofSeconds(300), recovery.getInitialDelay());
        assertEquals(Duration.ofSeconds(1800), recovery.getFixedDelay());
    }

    @Test
    @DisplayName("UT initialize() with negative batchSize should assign default batchSize")
    public void initialize_negativeBatchSize_shouldUseDefault() {
        // given
        OutboxProperties.StuckRecoveryProperties recovery = new OutboxProperties.StuckRecoveryProperties();
        recovery.setBatchSize(-10);
        recovery.setInitialDelay(Duration.ofSeconds(100));
        recovery.setFixedDelay(Duration.ofSeconds(200));

        // when
        recovery.initialize();

        // then
        assertEquals(100, recovery.getBatchSize());
        assertEquals(Duration.ofSeconds(100), recovery.getInitialDelay());
        assertEquals(Duration.ofSeconds(200), recovery.getFixedDelay());
    }

    @Test
    @DisplayName("UT initialize() with valid parameters should assign provided values")
    public void initialize_validParameters_shouldAssignValues() {
        // given
        int batchSize = 50;
        Duration initialDelay = Duration.ofSeconds(60);
        Duration fixedDelay = Duration.ofSeconds(120);

        OutboxProperties.StuckRecoveryProperties recovery = new OutboxProperties.StuckRecoveryProperties();
        recovery.setBatchSize(batchSize);
        recovery.setInitialDelay(initialDelay);
        recovery.setFixedDelay(fixedDelay);

        // when
        recovery.initialize();

        // then
        assertEquals(batchSize, recovery.getBatchSize());
        assertEquals(initialDelay, recovery.getInitialDelay());
        assertEquals(fixedDelay, recovery.getFixedDelay());
    }

    @Test
    @DisplayName("UT equals and hashCode should work correctly")
    public void equalsAndHashCode_shouldCompareCorrectly() {
        // given
        OutboxProperties.StuckRecoveryProperties recovery1 = new OutboxProperties.StuckRecoveryProperties();
        OutboxProperties.StuckRecoveryProperties recovery2 = new OutboxProperties.StuckRecoveryProperties();
        OutboxProperties.StuckRecoveryProperties recovery3 = new OutboxProperties.StuckRecoveryProperties();
        recovery3.setBatchSize(50);
        recovery3.setInitialDelay(Duration.ofSeconds(60));
        recovery3.setFixedDelay(Duration.ofSeconds(120));
        recovery3.initialize();

        // then
        assertEquals(recovery1, recovery2);
        assertEquals(recovery1.hashCode(), recovery2.hashCode());
        assertNotEquals(recovery1, recovery3);
        assertNotEquals(recovery1.hashCode(), recovery3.hashCode());
    }
}
