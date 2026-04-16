package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxPublisherProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class OutboxPropertiesStuckRecoveryPublisherPropertiesUnitTests {

    @Test
    @DisplayName("UT initialize() with no parameters should assign default values")
    public void init_noParams_shouldAssignDefaults() {
        // given + when
        OutboxPublisherProperties.StuckRecoveryProperties recovery = new OutboxPublisherProperties.StuckRecoveryProperties();
        recovery.init();

        // then
        assertEquals(500, recovery.getBatchSize());
        assertEquals(Duration.ofSeconds(300), recovery.getInitialDelay());
        assertEquals(Duration.ofSeconds(60), recovery.getFixedDelay());
    }

    @Test
    @DisplayName("UT initialize() with null parameters should assign default values")
    public void init_nullParameters_shouldAssignDefaults() {
        // given
        OutboxPublisherProperties.StuckRecoveryProperties recovery = new OutboxPublisherProperties.StuckRecoveryProperties();
        recovery.setBatchSize(null);
        recovery.setInitialDelay(null);
        recovery.setFixedDelay(null);

        // when
        recovery.init();

        // then
        assertEquals(500, recovery.getBatchSize());
        assertEquals(Duration.ofSeconds(300), recovery.getMaxBatchProcessingTime());
        assertEquals(Duration.ofSeconds(300), recovery.getInitialDelay());
        assertEquals(Duration.ofSeconds(60), recovery.getFixedDelay());
    }

    @Test
    @DisplayName("UT initialize() with negative batchSize should assign default batchSize")
    public void init_negativeBatchSize_shouldUseDefault() {
        // given
        OutboxPublisherProperties.StuckRecoveryProperties recovery = new OutboxPublisherProperties.StuckRecoveryProperties();
        recovery.setBatchSize(-10);
        recovery.setInitialDelay(Duration.ofSeconds(100));
        recovery.setFixedDelay(Duration.ofSeconds(200));

        // when
        recovery.init();

        // then
        assertEquals(500, recovery.getBatchSize());
        assertEquals(Duration.ofSeconds(300), recovery.getMaxBatchProcessingTime());
        assertEquals(Duration.ofSeconds(100), recovery.getInitialDelay());
        assertEquals(Duration.ofSeconds(200), recovery.getFixedDelay());
    }

    @Test
    @DisplayName("UT initialize() with valid parameters should assign provided values")
    public void init_validParameters_shouldAssignValues() {
        // given
        int batchSize = 50;
        Duration initialDelay = Duration.ofSeconds(60);
        Duration fixedDelay = Duration.ofSeconds(120);

        OutboxPublisherProperties.StuckRecoveryProperties recovery = new OutboxPublisherProperties.StuckRecoveryProperties();
        recovery.setBatchSize(batchSize);
        recovery.setInitialDelay(initialDelay);
        recovery.setFixedDelay(fixedDelay);

        // when
        recovery.init();

        // then
        assertEquals(batchSize, recovery.getBatchSize());
        assertEquals(Duration.ofSeconds(300), recovery.getMaxBatchProcessingTime());
        assertEquals(initialDelay, recovery.getInitialDelay());
        assertEquals(fixedDelay, recovery.getFixedDelay());
    }

    @Test
    @DisplayName("UT equals and hashCode should work correctly")
    public void equalsAndHashCode_shouldCompareCorrectly() {
        // given
        OutboxPublisherProperties.StuckRecoveryProperties recovery1 = new OutboxPublisherProperties.StuckRecoveryProperties();
        OutboxPublisherProperties.StuckRecoveryProperties recovery2 = new OutboxPublisherProperties.StuckRecoveryProperties();
        OutboxPublisherProperties.StuckRecoveryProperties recovery3 = new OutboxPublisherProperties.StuckRecoveryProperties();
        recovery3.setBatchSize(50);
        recovery3.setInitialDelay(Duration.ofSeconds(60));
        recovery3.setFixedDelay(Duration.ofSeconds(120));
        recovery3.init();

        // then
        assertEquals(recovery1, recovery2);
        assertEquals(recovery1.hashCode(), recovery2.hashCode());
        assertNotEquals(recovery1, recovery3);
        assertNotEquals(recovery1.hashCode(), recovery3.hashCode());
    }
}
