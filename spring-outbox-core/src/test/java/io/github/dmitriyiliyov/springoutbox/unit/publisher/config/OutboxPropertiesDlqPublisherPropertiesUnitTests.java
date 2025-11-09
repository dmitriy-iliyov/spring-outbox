package io.github.dmitriyiliyov.springoutbox.unit.publisher.config;

import io.github.dmitriyiliyov.springoutbox.publisher.config.OutboxPublisherProperties.DlqProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class OutboxPropertiesDlqPublisherPropertiesUnitTests {

    @Test
    @DisplayName("UT initialize() with enabled = true and null parameters should assign defaults")
    public void initialize_enabledTrue_nullParameters_shouldAssignDefaults() {
        // given
        DlqProperties dlq = new DlqProperties();
        dlq.setEnabled(true);
        dlq.setBatchSize(null);
        dlq.setTransferToInitialDelay(null);
        dlq.setTransferToFixedDelay(null);
        dlq.setTransferFromInitialDelay(null);
        dlq.setTransferFromFixedDelay(null);

        // when
        dlq.initialize();

        // then
        assertTrue(dlq.isEnabled());
        assertEquals(100, dlq.getBatchSize());
        assertEquals(Duration.ofSeconds(300), dlq.getTransferToInitialDelay());
        assertEquals(Duration.ofSeconds(900), dlq.getTransferToFixedDelay());
        assertEquals(Duration.ofSeconds(300), dlq.getTransferFromInitialDelay());
        assertEquals(Duration.ofSeconds(3600), dlq.getTransferFromFixedDelay());
    }

    @Test
    @DisplayName("UT initialize() with enabled = true and valid parameters should assign values")
    public void initialize_enabledTrue_withValues_shouldAssignValues() {
        // given
        DlqProperties dlq = new DlqProperties();
        dlq.setEnabled(true);
        dlq.setBatchSize(50);
        dlq.setTransferToInitialDelay(Duration.ofSeconds(100));
        dlq.setTransferToFixedDelay(Duration.ofSeconds(200));
        dlq.setTransferFromInitialDelay(Duration.ofSeconds(150));
        dlq.setTransferFromFixedDelay(Duration.ofSeconds(300));

        // when
        dlq.initialize();

        // then
        assertTrue(dlq.isEnabled());
        assertEquals(50, dlq.getBatchSize());
        assertEquals(Duration.ofSeconds(100), dlq.getTransferToInitialDelay());
        assertEquals(Duration.ofSeconds(200), dlq.getTransferToFixedDelay());
        assertEquals(Duration.ofSeconds(150), dlq.getTransferFromInitialDelay());
        assertEquals(Duration.ofSeconds(300), dlq.getTransferFromFixedDelay());
    }

    @Test
    @DisplayName("UT initialize() with enabled = true and partial null/invalid parameters should assign defaults for those")
    public void initialize_enabledTrue_partialNullOrInvalid_shouldAssignDefaults() {
        // given
        DlqProperties dlq = new DlqProperties();
        dlq.setEnabled(true);
        dlq.setBatchSize(-1);
        dlq.setTransferToInitialDelay(null);
        dlq.setTransferToFixedDelay(Duration.ofSeconds(50));
        dlq.setTransferFromInitialDelay(null);
        dlq.setTransferFromFixedDelay(null);

        // when
        dlq.initialize();

        // then
        assertTrue(dlq.isEnabled());
        assertEquals(100, dlq.getBatchSize());
        assertEquals(Duration.ofSeconds(300), dlq.getTransferToInitialDelay());
        assertEquals(Duration.ofSeconds(50), dlq.getTransferToFixedDelay());
        assertEquals(Duration.ofSeconds(300), dlq.getTransferFromInitialDelay());
        assertEquals(Duration.ofSeconds(3600), dlq.getTransferFromFixedDelay());
    }

    @Test
    @DisplayName("UT initialize() with enabled = false should disable DLQ and set nulls")
    public void initialize_enabledFalse_shouldDisableAndSetNulls() {
        // given
        DlqProperties dlq = new DlqProperties();
        dlq.setEnabled(false);
        dlq.setBatchSize(50);
        dlq.setTransferToInitialDelay(Duration.ofSeconds(10));
        dlq.setTransferToFixedDelay(Duration.ofSeconds(20));
        dlq.setTransferFromInitialDelay(Duration.ofSeconds(30));
        dlq.setTransferFromFixedDelay(Duration.ofSeconds(40));

        // when
        dlq.initialize();

        // then
        assertFalse(dlq.isEnabled());
        assertEquals(0, dlq.getBatchSize());
        assertNull(dlq.getTransferToInitialDelay());
        assertNull(dlq.getTransferToFixedDelay());
        assertNull(dlq.getTransferFromInitialDelay());
        assertNull(dlq.getTransferFromFixedDelay());
    }

    @Test
    @DisplayName("UT initialize() with enabled = null should treat as disabled and set nulls")
    public void initialize_enabledNull_shouldDisableAndSetNulls() {
        // given
        DlqProperties dlq = new DlqProperties();
        dlq.setEnabled(null);
        dlq.setBatchSize(null);
        dlq.setTransferToInitialDelay(null);
        dlq.setTransferToFixedDelay(null);
        dlq.setTransferFromInitialDelay(null);
        dlq.setTransferFromFixedDelay(null);

        // when
        dlq.initialize();

        // then
        assertFalse(dlq.isEnabled());
        assertEquals(0, dlq.getBatchSize());
        assertNull(dlq.getTransferToInitialDelay());
        assertNull(dlq.getTransferToFixedDelay());
        assertNull(dlq.getTransferFromInitialDelay());
        assertNull(dlq.getTransferFromFixedDelay());
    }
}
