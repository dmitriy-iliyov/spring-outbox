package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OutboxPublisherPropertiesDefaultsUnitTests {

    @Test
    @DisplayName("UT init() with all null parameters should assign default values")
    public void init_allNull_shouldAssignDefaults() {
        // given
        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        defaults.setBatchSize(null);
        defaults.setInitialDelay(null);
        defaults.setFixedDelay(null);
        defaults.setMaxRetries(null);
        defaults.setBackoff(null);

        // when
        defaults.init();

        // then
        assertEquals(50, defaults.getBatchSize());
        assertEquals(Duration.ofSeconds(300), defaults.getInitialDelay());
        assertEquals(Duration.ofSeconds(2), defaults.getFixedDelay());
        assertEquals(3, defaults.getMaxRetries());
        assertNotNull(defaults.getBackoff());
    }

    @Test
    @DisplayName("UT init() with batchSize <= 0 should assign default batch size")
    public void init_batchSizeInvalid_shouldAssignDefault() {
        // given
        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        defaults.setBatchSize(0);
        defaults.setInitialDelay(Duration.ofSeconds(1));
        defaults.setFixedDelay(Duration.ofSeconds(1));
        defaults.setMaxRetries(1);
        defaults.setBackoff(new OutboxPublisherProperties.BackoffProperties());

        // when
        defaults.init();

        // then
        assertEquals(50, defaults.getBatchSize());
    }

    @Test
    @DisplayName("UT init() with maxRetries < 0 should assign default maxRetries")
    public void init_maxRetriesInvalid_shouldAssignDefault() {
        // given
        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        defaults.setBatchSize(10);
        defaults.setInitialDelay(Duration.ofSeconds(1));
        defaults.setFixedDelay(Duration.ofSeconds(1));
        defaults.setMaxRetries(-1);
        defaults.setBackoff(new OutboxPublisherProperties.BackoffProperties());

        // when
        defaults.init();

        // then
        assertEquals(3, defaults.getMaxRetries());
    }

    @Test
    @DisplayName("UT init() with valid values provided should keep provided values")
    public void init_validValues_shouldKeepProvided() {
        // given
        OutboxPublisherProperties.BackoffProperties customBackoff = new OutboxPublisherProperties.BackoffProperties();
        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        defaults.setBatchSize(100);
        defaults.setInitialDelay(Duration.ofSeconds(10));
        defaults.setFixedDelay(Duration.ofSeconds(5));
        defaults.setMaxRetries(7);
        defaults.setBackoff(customBackoff);

        // when
        defaults.init();

        // then
        assertEquals(100, defaults.getBatchSize());
        assertEquals(Duration.ofSeconds(10), defaults.getInitialDelay());
        assertEquals(Duration.ofSeconds(5), defaults.getFixedDelay());
        assertEquals(7, defaults.getMaxRetries());
        assertEquals(customBackoff, defaults.getBackoff());
    }

    @Test
    @DisplayName("UT init() with null backoff should assign default backoff")
    public void init_backoffNull_shouldAssignDefault() {
        // given
        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        defaults.setBatchSize(10);
        defaults.setInitialDelay(Duration.ofSeconds(1));
        defaults.setFixedDelay(Duration.ofSeconds(1));
        defaults.setMaxRetries(2);
        defaults.setBackoff(null);

        // when
        defaults.init();

        // then
        assertNotNull(defaults.getBackoff());
    }

    @Test
    @DisplayName("UT init() with null initialDelay and fixedDelay should assign defaults")
    public void init_delaysNull_shouldAssignDefaults() {
        // given
        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        defaults.setBatchSize(10);
        defaults.setInitialDelay(null);
        defaults.setFixedDelay(null);
        defaults.setMaxRetries(2);
        defaults.setBackoff(new OutboxPublisherProperties.BackoffProperties());

        // when
        defaults.init();

        // then
        assertEquals(Duration.ofSeconds(300), defaults.getInitialDelay());
        assertEquals(Duration.ofSeconds(2), defaults.getFixedDelay());
    }
}
