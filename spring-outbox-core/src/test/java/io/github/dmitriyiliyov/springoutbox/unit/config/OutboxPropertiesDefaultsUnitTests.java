package io.github.dmitriyiliyov.springoutbox.unit.config;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OutboxPropertiesDefaultsUnitTests {

    @Test
    @DisplayName("UT initialize() with all null parameters should assign default values")
    public void initialize_allNull_shouldAssignDefaults() {
        // given
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults();
        defaults.setBatchSize(null);
        defaults.setInitialDelay(null);
        defaults.setFixedDelay(null);
        defaults.setMaxRetries(null);
        defaults.setBackoff(null);

        // when
        defaults.initialize();

        // then
        assertEquals(50, defaults.getBatchSize());
        assertEquals(Duration.ofSeconds(300), defaults.getInitialDelay());
        assertEquals(Duration.ofSeconds(2), defaults.getFixedDelay());
        assertEquals(3, defaults.getMaxRetries());
        assertNotNull(defaults.getBackoff());
    }

    @Test
    @DisplayName("UT initialize() with batchSize <= 0 should assign default batch size")
    public void initialize_batchSizeInvalid_shouldAssignDefault() {
        // given
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults();
        defaults.setBatchSize(0);
        defaults.setInitialDelay(Duration.ofSeconds(1));
        defaults.setFixedDelay(Duration.ofSeconds(1));
        defaults.setMaxRetries(1);
        defaults.setBackoff(new OutboxProperties.BackoffProperties());

        // when
        defaults.initialize();

        // then
        assertEquals(50, defaults.getBatchSize());
    }

    @Test
    @DisplayName("UT initialize() with maxRetries < 0 should assign default maxRetries")
    public void initialize_maxRetriesInvalid_shouldAssignDefault() {
        // given
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults();
        defaults.setBatchSize(10);
        defaults.setInitialDelay(Duration.ofSeconds(1));
        defaults.setFixedDelay(Duration.ofSeconds(1));
        defaults.setMaxRetries(-1);
        defaults.setBackoff(new OutboxProperties.BackoffProperties());

        // when
        defaults.initialize();

        // then
        assertEquals(3, defaults.getMaxRetries());
    }

    @Test
    @DisplayName("UT initialize() with valid values provided should keep provided values")
    public void initialize_validValues_shouldKeepProvided() {
        // given
        OutboxProperties.BackoffProperties customBackoff = new OutboxProperties.BackoffProperties();
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults();
        defaults.setBatchSize(100);
        defaults.setInitialDelay(Duration.ofSeconds(10));
        defaults.setFixedDelay(Duration.ofSeconds(5));
        defaults.setMaxRetries(7);
        defaults.setBackoff(customBackoff);

        // when
        defaults.initialize();

        // then
        assertEquals(100, defaults.getBatchSize());
        assertEquals(Duration.ofSeconds(10), defaults.getInitialDelay());
        assertEquals(Duration.ofSeconds(5), defaults.getFixedDelay());
        assertEquals(7, defaults.getMaxRetries());
        assertEquals(customBackoff, defaults.getBackoff());
    }

    @Test
    @DisplayName("UT initialize() with null backoff should assign default backoff")
    public void initialize_backoffNull_shouldAssignDefault() {
        // given
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults();
        defaults.setBatchSize(10);
        defaults.setInitialDelay(Duration.ofSeconds(1));
        defaults.setFixedDelay(Duration.ofSeconds(1));
        defaults.setMaxRetries(2);
        defaults.setBackoff(null);

        // when
        defaults.initialize();

        // then
        assertNotNull(defaults.getBackoff());
    }

    @Test
    @DisplayName("UT initialize() with null initialDelay and fixedDelay should assign defaults")
    public void initialize_delaysNull_shouldAssignDefaults() {
        // given
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults();
        defaults.setBatchSize(10);
        defaults.setInitialDelay(null);
        defaults.setFixedDelay(null);
        defaults.setMaxRetries(2);
        defaults.setBackoff(new OutboxProperties.BackoffProperties());

        // when
        defaults.initialize();

        // then
        assertEquals(Duration.ofSeconds(300), defaults.getInitialDelay());
        assertEquals(Duration.ofSeconds(2), defaults.getFixedDelay());
    }
}
