package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.starter.OutboxProperties;
import io.github.dmitriyiliyov.springoutbox.starter.PollingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class OutboxPublisherPropertiesUnitTests {

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should throw when sender is null")
    public void applyDefaults_whenSenderNull_thenThrow() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        properties.setEvents(new HashMap<>());

        // when + then
        assertThrows(IllegalArgumentException.class, properties::applyDefaults);
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should throw when events is null")
    public void applyDefaults_whenEventsNull_thenThrow() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("beanName");
        properties.setSender(sender);

        // when + then
        assertThrows(IllegalArgumentException.class, properties::applyDefaults);
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should accept empty events map")
    public void applyDefaults_whenEventsEmpty_thenAccept() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("beanName");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        // when
        properties.applyDefaults();

        // then
        assertTrue(properties.getEvents().isEmpty());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should throw when event type is null")
    public void applyDefaults_whenEventTypeNull_thenThrow() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("beanName");
        properties.setSender(sender);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("topic");
        events.put(null, event);
        properties.setEvents(events);

        // when + then
        assertThrows(IllegalArgumentException.class, properties::applyDefaults);
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should throw when event type is blank")
    public void applyDefaults_whenEventTypeBlank_thenThrow() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("beanName");
        properties.setSender(sender);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("topic");
        events.put("  ", event);
        properties.setEvents(events);

        // when + then
        assertThrows(IllegalArgumentException.class, properties::applyDefaults);
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should applyDefaults stuckRecovery when null")
    public void applyDefaults_whenStuckRecoveryNull_thenUseDefault() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("beanName");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        // when
        properties.applyDefaults();

        // then
        assertNotNull(properties.getStuckRecovery());
        assertEquals(500, properties.getStuckRecovery().getBatchSize());
        assertEquals(Duration.ofMinutes(5), properties.getStuckRecovery().getMaxBatchProcessingTime());
        assertEquals(PollingType.ADAPTIVE, properties.getDefaults().getPolling().getType());
        assertEquals(Duration.ofMinutes(5), properties.getDefaults().getPolling().getInitialDelay());
        assertEquals(Duration.ofMillis(250), properties.getDefaults().getPolling().getMinFixedDelay());
        assertEquals(Duration.ofMinutes(1), properties.getDefaults().getPolling().getMaxFixedDelay());
        assertEquals(1.5, properties.getDefaults().getPolling().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should applyDefaults defaults when null")
    public void applyDefaults_whenDefaultsNull_thenUseDefault() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("beanName");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        // when
        properties.applyDefaults();

        // then
        assertNotNull(properties.getDefaults());
        assertEquals(200, properties.getDefaults().getBatchSize());
        assertEquals(PollingType.ADAPTIVE, properties.getDefaults().getPolling().getType());
        assertEquals(Duration.ofMinutes(5), properties.getDefaults().getPolling().getInitialDelay());
        assertEquals(Duration.ofMillis(250), properties.getDefaults().getPolling().getMinFixedDelay());
        assertEquals(Duration.ofMinutes(1), properties.getDefaults().getPolling().getMaxFixedDelay());
        assertEquals(1.5, properties.getDefaults().getPolling().getMultiplier());
        assertEquals(3, properties.getDefaults().getMaxRetries());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() with full configuration should applyDefaults all properties correctly")
    public void applyDefaults_withFullConfiguration_shouldApplyDefaultsAll() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("kafkaOutboxTemplate");
        properties.setSender(sender);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();

        OutboxPublisherProperties.EventProperties accountDelete = new OutboxPublisherProperties.EventProperties();
        accountDelete.setTopic("account-delete");
        OutboxProperties.PollingProperties polling1 = new OutboxProperties.PollingProperties();
        polling1.setType(PollingType.FIXED);
        polling1.setFixedDelay(Duration.ofSeconds(10));
        accountDelete.setPolling(polling1);
        accountDelete.setMaxRetries(1);
        events.put("account-delete", accountDelete);

        OutboxPublisherProperties.EventProperties userRegistered = new OutboxPublisherProperties.EventProperties();
        userRegistered.setTopic("user-registered");
        userRegistered.setMaxRetries(4);
        OutboxPublisherProperties.BackoffProperties backoff = new OutboxPublisherProperties.BackoffProperties();
        userRegistered.setBackoff(backoff);
        events.put("user-registered", userRegistered);

        properties.setEvents(events);

        OutboxPublisherProperties.StuckRecoveryProperties stuckRecovery = new OutboxPublisherProperties.StuckRecoveryProperties();
        stuckRecovery.setMaxBatchProcessingTime(Duration.ofMinutes(10));
        properties.setStuckRecovery(stuckRecovery);

        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        properties.setCleanUp(cleanUp);

        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        dlq.setEnabled(true);
        properties.setDlq(dlq);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        properties.setDefaults(defaults);

        // when
        properties.applyDefaults();

        // then
        assertEquals(SenderType.KAFKA, properties.getSender().getType());
        assertEquals("kafkaOutboxTemplate", properties.getSender().getBeanName());

        assertEquals(200, properties.getEvents().get("account-delete").getBatchSize());
        assertEquals(PollingType.FIXED, properties.getEvents().get("account-delete").getPolling().getType());
        assertEquals(Duration.ofMinutes(5), properties.getEvents().get("account-delete").getPolling().getInitialDelay());
        assertEquals(Duration.ofSeconds(10), properties.getEvents().get("account-delete").getPolling().getFixedDelay());
        assertEquals(Duration.ZERO, properties.getEvents().get("account-delete").getPolling().getMinFixedDelay());
        assertEquals(Duration.ZERO, properties.getEvents().get("account-delete").getPolling().getMaxFixedDelay());
        assertEquals(Double.NaN, properties.getEvents().get("account-delete").getPolling().getMultiplier());
        assertEquals(1, properties.getEvents().get("account-delete").getMaxRetries());

        assertEquals(200, properties.getEvents().get("user-registered").getBatchSize());
        assertEquals(PollingType.ADAPTIVE, properties.getEvents().get("user-registered").getPolling().getType());
        assertEquals(Duration.ofMinutes(5), properties.getEvents().get("user-registered").getInitialDelay());
        assertEquals(Duration.ZERO, properties.getEvents().get("user-registered").getFixedDelay());
        assertEquals(Duration.ofMillis(250), properties.getEvents().get("user-registered").getPolling().getMinFixedDelay());
        assertEquals(Duration.ofMinutes(1), properties.getEvents().get("user-registered").getPolling().getMaxFixedDelay());
        assertEquals(1.5, properties.getEvents().get("user-registered").getPolling().getMultiplier());
        assertEquals(4, properties.getEvents().get("user-registered").getMaxRetries());

        assertEquals(500, properties.getStuckRecovery().getBatchSize());
        assertEquals(Duration.ofMinutes(10), properties.getStuckRecovery().getMaxBatchProcessingTime());
        assertEquals(PollingType.ADAPTIVE, properties.getStuckRecovery().getPolling().getType());
        assertEquals(Duration.ofMinutes(5), properties.getStuckRecovery().getInitialDelay());
        assertEquals(Duration.ZERO, properties.getStuckRecovery().getFixedDelay());
        assertEquals(Duration.ofSeconds(1), properties.getStuckRecovery().getPolling().getMinFixedDelay());
        assertEquals(Duration.ofMinutes(1), properties.getStuckRecovery().getPolling().getMaxFixedDelay());
        assertEquals(4, properties.getStuckRecovery().getPolling().getMultiplier());

        assertTrue(properties.isCleanUpEnabled());
        assertEquals(500, properties.getCleanUp().getBatchSize());
        assertEquals(Duration.ofHours(24), properties.getCleanUp().getTtl());
        assertEquals(PollingType.ADAPTIVE, properties.getCleanUp().getPolling().getType());
        assertEquals(Duration.ofMinutes(5), properties.getCleanUp().getInitialDelay());
        assertEquals(Duration.ZERO, properties.getCleanUp().getFixedDelay());
        assertEquals(Duration.ofSeconds(5), properties.getCleanUp().getPolling().getMinFixedDelay());
        assertEquals(Duration.ofMinutes(1), properties.getCleanUp().getPolling().getMaxFixedDelay());
        assertEquals(2, properties.getCleanUp().getPolling().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should handle disabled backoff in defaults correctly")
    public void applyDefaults_whenBackoffDisabledInDefaults_thenApplyCorrectly() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("kafkaOutboxTemplate");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(false);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();

        OutboxPublisherProperties.EventProperties accountDelete = new OutboxPublisherProperties.EventProperties();
        accountDelete.setTopic("account-delete");
        OutboxProperties.PollingProperties polling1 = new OutboxProperties.PollingProperties();
        polling1.setFixedDelay(Duration.ofSeconds(10));
        accountDelete.setPolling(polling1);
        accountDelete.setMaxRetries(1);
        events.put("account-delete", accountDelete);

        OutboxPublisherProperties.EventProperties userRegistered = new OutboxPublisherProperties.EventProperties();
        userRegistered.setTopic("user-registered");
        userRegistered.setMaxRetries(4);
        OutboxPublisherProperties.BackoffProperties backoff = new OutboxPublisherProperties.BackoffProperties();
        userRegistered.setBackoff(backoff);
        events.put("user-registered", userRegistered);

        properties.setEvents(events);

        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        cleanUp.setEnabled(true);
        properties.setCleanUp(cleanUp);

        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        dlq.setEnabled(true);
        properties.setDlq(dlq);

        // when
        properties.applyDefaults();

        // then
        assertFalse(properties.getDefaults().getBackoff().isEnabled());
        assertFalse(properties.getEvents().get("account-delete").getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(0), properties.getEvents().get("account-delete").getBackoff().getDelay());
        assertEquals(1.0, properties.getEvents().get("account-delete").getBackoff().getMultiplier());

        assertTrue(properties.getEvents().get("user-registered").getBackoff().isEnabled());
        assertTrue(properties.isCleanUpEnabled());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should apply defaults to event properties with null values")
    public void applyDefaults_whenEventPropertiesNull_thenApplyDefaults() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        defaults.setBatchSize(50);
        OutboxProperties.PollingProperties defaultPolling = new OutboxProperties.PollingProperties();
        defaultPolling.setType(PollingType.FIXED);
        defaultPolling.setInitialDelay(Duration.ofSeconds(10));
        defaultPolling.setFixedDelay(Duration.ofSeconds(60));
        defaults.setPolling(defaultPolling);
        defaults.setMaxRetries(5);
        OutboxPublisherProperties.BackoffProperties backoff = new OutboxPublisherProperties.BackoffProperties();
        backoff.setEnabled(true);
        backoff.setDelay(Duration.ofSeconds(5));
        backoff.setMultiplier(3.9);
        defaults.setBackoff(backoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.applyDefaults();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertEquals(50, resultEvent.getBatchSize());
        assertEquals(Duration.ofSeconds(10), resultEvent.getInitialDelay());
        assertEquals(Duration.ofSeconds(60), resultEvent.getFixedDelay());
        assertEquals(5, resultEvent.getMaxRetries());
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(5), resultEvent.getBackoff().getDelay());
        assertEquals(3.9, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should override defaults with event specific values")
    public void applyDefaults_whenEventPropertiesProvided_thenOverrideDefaults() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        defaults.setBatchSize(50);
        OutboxProperties.PollingProperties defaultPolling = new OutboxProperties.PollingProperties();
        defaultPolling.setType(PollingType.FIXED);
        defaultPolling.setInitialDelay(Duration.ofSeconds(10));
        defaultPolling.setFixedDelay(Duration.ofSeconds(60));
        defaults.setPolling(defaultPolling);
        defaults.setMaxRetries(5);
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(5));
        defaultBackoff.setMultiplier(3.9);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        event.setBatchSize(100);
        OutboxProperties.PollingProperties eventPolling = new OutboxProperties.PollingProperties();
        eventPolling.setInitialDelay(Duration.ofSeconds(20));
        eventPolling.setFixedDelay(Duration.ofSeconds(120));
        event.setPolling(eventPolling);
        event.setMaxRetries(10);
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(15));
        eventBackoff.setMultiplier(5.4);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.applyDefaults();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertEquals(100, resultEvent.getBatchSize());
        assertEquals(Duration.ofSeconds(20), resultEvent.getInitialDelay());
        assertEquals(Duration.ofSeconds(120), resultEvent.getFixedDelay());
        assertEquals(10, resultEvent.getMaxRetries());
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(15), resultEvent.getBackoff().getDelay());
        assertEquals(5.4, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should handle disabled backoff in event properties")
    public void applyDefaults_whenEventBackoffDisabled_thenApplyCorrectly() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        defaults.setBatchSize(50);
        OutboxProperties.PollingProperties defaultPolling = new OutboxProperties.PollingProperties();
        defaultPolling.setInitialDelay(Duration.ofSeconds(10));
        defaultPolling.setFixedDelay(Duration.ofSeconds(60));
        defaults.setPolling(defaultPolling);
        defaults.setMaxRetries(5);
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(5));
        defaultBackoff.setMultiplier(3.9);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(false);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.applyDefaults();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertFalse(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(0), resultEvent.getBackoff().getDelay());
        assertEquals(1.0, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should use default backoff delay when event backoff delay is null")
    public void applyDefaults_whenEventBackoffDelayNull_thenUseDefaultDelay() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        OutboxProperties.PollingProperties defaultPolling = new OutboxProperties.PollingProperties();
        defaultPolling.setType(PollingType.FIXED);
        defaultPolling.setInitialDelay(Duration.ofSeconds(10));
        defaults.setPolling(defaultPolling);
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(3.9);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setMultiplier(5.4);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.applyDefaults();

        System.out.println(defaults);
        System.out.println(defaultBackoff);
        System.out.println(eventBackoff);
        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(15), resultEvent.getBackoff().getDelay());
        assertEquals(5.4, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should use event backoff delay when provided")
    public void applyDefaults_whenEventBackoffDelayProvided_thenUseEventDelay() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(3.9);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(25));
        eventBackoff.setMultiplier(5.4);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.applyDefaults();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(25), resultEvent.getBackoff().getDelay());
        assertEquals(5.4, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should use default backoff multiplier when event multiplier is zero")
    public void applyDefaults_whenEventBackoffMultiplierZero_thenUseDefaultMultiplier() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(3.9);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(20));
        eventBackoff.setMultiplier(0.0);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.applyDefaults();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(20), resultEvent.getBackoff().getDelay());
        assertEquals(3.9, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should use default backoff multiplier when event multiplier is negative")
    public void applyDefaults_whenEventBackoffMultiplierNegative_thenUseDefaultMultiplier() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(3.9);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(20));
        eventBackoff.setMultiplier(-2.8);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.applyDefaults();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(20), resultEvent.getBackoff().getDelay());
        assertEquals(3.9, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should use event backoff multiplier when it is 1")
    public void applyDefaults_whenEventBackoffMultiplierOne_thenUseEventMultiplier() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(3.9);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(20));
        eventBackoff.setMultiplier(1.0);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.applyDefaults();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(20), resultEvent.getBackoff().getDelay());
        assertEquals(1.0, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should use event backoff multiplier when greater than 1")
    public void applyDefaults_whenEventBackoffMultiplierGreaterThanOne_thenUseEventMultiplier() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(3.9);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(20));
        eventBackoff.setMultiplier(7.3);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.applyDefaults();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(20), resultEvent.getBackoff().getDelay());
        assertEquals(7.3, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should use both defaults when event backoff delay and multiplier are null")
    public void applyDefaults_whenEventBackoffDelayAndMultiplierNull_thenUseBothDefaults() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        OutboxProperties.PollingProperties defaultPolling = new OutboxProperties.PollingProperties();
        defaultPolling.setType(PollingType.FIXED);
        defaultPolling.setInitialDelay(Duration.ofSeconds(10));
        defaults.setPolling(defaultPolling);

        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(2.6);
        defaults.setBackoff(defaultBackoff);

        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.applyDefaults();

        System.out.println(defaultBackoff);
        System.out.println(defaults);
        System.out.println(properties.getEvents().get("test-event"));
        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(15), resultEvent.getBackoff().getDelay());
        assertEquals(2.6, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should use both event values when backoff delay and multiplier are provided")
    public void applyDefaults_whenEventBackoffDelayAndMultiplierProvided_thenUseBothEventValues() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(3.9);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(25));
        eventBackoff.setMultiplier(6.4);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.applyDefaults();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(25), resultEvent.getBackoff().getDelay());
        assertEquals(6.4, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.applyDefaults() should mix defaults and event values correctly")
    public void applyDefaults_whenMixingDefaultsAndEventValues_thenApplyCorrectly() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(4.0);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(25));
        eventBackoff.setMultiplier(0.0);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.applyDefaults();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(25), resultEvent.getBackoff().getDelay());
        assertEquals(4.0, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT isCleanUpEnabled() should return true when cleanUp is null")
    public void isCleanUpEnabled_whenCleanUpNull_thenReturnTrue() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        // when
        properties.applyDefaults();

        // then
        assertTrue(properties.isCleanUpEnabled());
    }

    @Test
    @DisplayName("UT isCleanUpEnabled() should return true when cleanUp enabled")
    public void isCleanUpEnabled_whenCleanUpEnabled_thenReturnTrue() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        cleanUp.setEnabled(true);
        properties.setCleanUp(cleanUp);

        // when
        properties.applyDefaults();

        // then
        assertTrue(properties.isCleanUpEnabled());
    }

    @Test
    @DisplayName("UT isCleanUpEnabled() should return false when cleanUp disabled")
    public void isCleanUpEnabled_whenCleanUpDisabled_thenReturnFalse() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        cleanUp.setEnabled(false);
        properties.setCleanUp(cleanUp);

        // when
        properties.applyDefaults();

        // then
        assertFalse(properties.isCleanUpEnabled());
    }

    @Test
    @DisplayName("UT getCleanUp() should return cleanUp properties when provided")
    public void getCleanUp_whenCleanUpProvided_thenReturnProperties() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        OutboxProperties.PollingProperties polling = new OutboxProperties.PollingProperties();
        polling.setType(PollingType.FIXED);
        polling.setInitialDelay(Duration.ofMinutes(5));
        polling.setFixedDelay(Duration.ofMillis(30));
        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        cleanUp.setEnabled(true);
        cleanUp.setBatchSize(50);
        cleanUp.setTtl(Duration.ofHours(2));
        cleanUp.setPolling(polling);
        properties.setCleanUp(cleanUp);

        // when
        properties.applyDefaults();

        // then
        assertNotNull(properties.getCleanUp());
        assertEquals(50, properties.getCleanUp().getBatchSize());
        assertEquals(Duration.ofHours(2), properties.getCleanUp().getTtl());
        assertEquals(Duration.ofMinutes(5), properties.getCleanUp().getInitialDelay());
        assertEquals(Duration.ofMillis(30), properties.getCleanUp().getFixedDelay());
    }

    @Test
    @DisplayName("UT existEventType() should return true when event exists")
    public void existEventType_whenEventExists_thenReturnTrue() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        event.setBatchSize(10);
        OutboxProperties.PollingProperties polling = new OutboxProperties.PollingProperties();
        polling.setInitialDelay(Duration.ofSeconds(5));
        polling.setFixedDelay(Duration.ofSeconds(30));
        event.setPolling(polling);
        event.setMaxRetries(3);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.applyDefaults();

        // then
        assertTrue(properties.existEventType("test-event"));
    }

    @Test
    @DisplayName("UT existEventType() should return false when event does not exist")
    public void existEventType_whenEventDoesNotExist_thenReturnFalse() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.applyDefaults();

        // then
        assertFalse(properties.existEventType("non-existing-event"));
    }

    @Test
    @DisplayName("UT getEvents() should return unmodifiable map")
    public void getEvents_shouldReturnUnmodifiableMap() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.applyDefaults();

        // then
        OutboxPublisherProperties.EventProperties newEvent = new OutboxPublisherProperties.EventProperties();
        newEvent.setTopic("topic");
        assertThrows(UnsupportedOperationException.class,
                () -> properties.getEvents().put("new-event", newEvent)
        );
    }

    @Test
    @DisplayName("UT getSender() should return sender properties")
    public void getSender_shouldReturnSenderProperties() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("testBean");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        // when
        properties.applyDefaults();

        // then
        assertNotNull(properties.getSender());
        assertEquals(SenderType.KAFKA, properties.getSender().getType());
        assertEquals("testBean", properties.getSender().getBeanName());
    }

    @Test
    @DisplayName("UT setEnabled() should update enabled flag")
    public void setEnabled_shouldUpdateEnabledFlag() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        properties.setEnabled(true);

        assertTrue(properties.isEnabled());

        properties.setEnabled(false);

        assertFalse(properties.isEnabled());
    }

    @Test
    @DisplayName("UT setMetrics() should update metrics properties")
    public void setMetrics_shouldUpdateMetricsProperties() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxProperties.MetricsProperties metrics = new OutboxProperties.MetricsProperties();

        // when
        properties.setMetrics(metrics);

        // then
        assertEquals(metrics, properties.getMetrics());
    }

    @Test
    @DisplayName("UT applyDefaults() when metrics is null should create enabled metrics")
    public void applyDefaults_whenMetricsNull_shouldCreateDefaultMetrics() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        // when
        properties.applyDefaults();

        // then
        assertNotNull(properties.getMetrics());
        assertFalse(properties.getMetrics().isEnabled());
        assertNotNull(properties.getMetrics().getGauge());
        assertFalse(properties.getMetrics().getGauge().isEnabled());
    }

    @Test
    @DisplayName("UT applyDefaults() when enabled is false should set enabled to false and apply default fallbacks")
    public void applyDefaultsEnabledFalse() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        properties.setEnabled(false);

        // when
        properties.applyDefaults();

        // then
        assertFalse(properties.isEnabled());
        assertFalse(properties.getCleanUp().isEnabled());
        assertFalse(properties.getDlq().isEnabled());
        assertThat(properties.getEvents()).isEmpty();
        assertFalse(properties.getMetrics().isEnabled());
        assertNull(properties.getSender());
    }
}
