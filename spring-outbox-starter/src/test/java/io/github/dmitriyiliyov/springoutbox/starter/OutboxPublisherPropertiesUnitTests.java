package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxPublisherProperties;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.SenderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class OutboxPublisherPropertiesUnitTests {

    @Test
    @DisplayName("UT OutboxProperties.initialize() should throw when sender is null")
    public void initialize_whenSenderNull_thenThrow() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        properties.setEvents(new HashMap<>());

        // when + then
        assertThrows(IllegalArgumentException.class, properties::afterPropertiesSet);
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should throw when events is null")
    public void initialize_whenEventsNull_thenThrow() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("beanName");
        properties.setSender(sender);

        // when + then
        assertThrows(IllegalArgumentException.class, properties::afterPropertiesSet);
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should accept empty events map")
    public void initialize_whenEventsEmpty_thenAccept() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("beanName");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        // when
        properties.afterPropertiesSet();

        // then
        assertTrue(properties.getEvents().isEmpty());
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should throw when event type is null")
    public void initialize_whenEventTypeNull_thenThrow() {
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
        assertThrows(IllegalArgumentException.class, properties::afterPropertiesSet);
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should throw when event type is blank")
    public void initialize_whenEventTypeBlank_thenThrow() {
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
        assertThrows(IllegalArgumentException.class, properties::afterPropertiesSet);
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should initialize stuckRecovery when null")
    public void initialize_whenStuckRecoveryNull_thenUseDefault() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("beanName");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        // when
        properties.afterPropertiesSet();

        // then
        assertNotNull(properties.getStuckRecovery());
        assertEquals(100, properties.getStuckRecovery().getBatchSize());
        assertEquals(Duration.ofSeconds(300), properties.getStuckRecovery().getInitialDelay());
        assertEquals(Duration.ofSeconds(1800), properties.getStuckRecovery().getFixedDelay());
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should initialize defaults when null")
    public void initialize_whenDefaultsNull_thenUseDefault() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("beanName");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        // when
        properties.afterPropertiesSet();

        // then
        assertNotNull(properties.getDefaults());
        assertEquals(50, properties.getDefaults().getBatchSize());
        assertEquals(Duration.ofSeconds(300), properties.getDefaults().getInitialDelay());
        assertEquals(Duration.ofSeconds(2), properties.getDefaults().getFixedDelay());
        assertEquals(3, properties.getDefaults().getMaxRetries());
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() with full configuration should initialize all properties correctly")
    public void initialize_withFullConfiguration_shouldInitializeAll() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("kafkaOutboxTemplate");
        properties.setSender(sender);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();

        OutboxPublisherProperties.EventProperties accountDelete = new OutboxPublisherProperties.EventProperties();
        accountDelete.setTopic("account-delete");
        accountDelete.setFixedDelay(Duration.ofSeconds(10));
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
        properties.setStuckRecovery(stuckRecovery);

        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        properties.setCleanUp(cleanUp);

        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        dlq.setEnabled(true);
        properties.setDlq(dlq);

        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        properties.setDefaults(defaults);

        // when
        properties.afterPropertiesSet();

        // then
        assertEquals(SenderType.KAFKA, properties.getSender().getType());
        assertEquals("kafkaOutboxTemplate", properties.getSender().getBeanName());

        assertEquals(50, properties.getEvents().get("account-delete").getBatchSize());
        assertEquals(Duration.ofSeconds(300), properties.getEvents().get("account-delete").getInitialDelay());
        assertEquals(Duration.ofSeconds(10), properties.getEvents().get("account-delete").getFixedDelay());
        assertEquals(1, properties.getEvents().get("account-delete").getMaxRetries());

        assertEquals(50, properties.getEvents().get("user-registered").getBatchSize());
        assertEquals(Duration.ofSeconds(300), properties.getEvents().get("user-registered").getInitialDelay());
        assertEquals(Duration.ofSeconds(2), properties.getEvents().get("user-registered").getFixedDelay());
        assertEquals(4, properties.getEvents().get("user-registered").getMaxRetries());

        assertEquals(100, properties.getStuckRecovery().getBatchSize());
        assertEquals(Duration.ofSeconds(300), properties.getStuckRecovery().getInitialDelay());
        assertEquals(Duration.ofSeconds(1800), properties.getStuckRecovery().getFixedDelay());

        assertTrue(properties.isCleanUpEnabled());
        assertEquals(100, properties.getCleanUp().getBatchSize());
        assertEquals(Duration.ofHours(1), properties.getCleanUp().getTtl());
        assertEquals(Duration.ofSeconds(120), properties.getCleanUp().getInitialDelay());
        assertEquals(Duration.ofSeconds(5), properties.getCleanUp().getFixedDelay());
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should handle disabled backoff in defaults correctly")
    public void initialize_whenBackoffDisabledInDefaults_thenApplyCorrectly() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("kafkaOutboxTemplate");
        properties.setSender(sender);

        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(false);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();

        OutboxPublisherProperties.EventProperties accountDelete = new OutboxPublisherProperties.EventProperties();
        accountDelete.setTopic("account-delete");
        accountDelete.setFixedDelay(Duration.ofSeconds(10));
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
        properties.afterPropertiesSet();

        // then
        assertFalse(properties.getDefaults().getBackoff().isEnabled());
        assertFalse(properties.getEvents().get("account-delete").getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(0), properties.getEvents().get("account-delete").getBackoff().getDelay());
        assertEquals(1, properties.getEvents().get("account-delete").getBackoff().getMultiplier());

        assertTrue(properties.getEvents().get("user-registered").getBackoff().isEnabled());
        assertTrue(properties.isCleanUpEnabled());
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should apply defaults to event properties with null values")
    public void initialize_whenEventPropertiesNull_thenApplyDefaults() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        defaults.setBatchSize(50);
        defaults.setInitialDelay(Duration.ofSeconds(10));
        defaults.setFixedDelay(Duration.ofSeconds(60));
        defaults.setMaxRetries(5);
        OutboxPublisherProperties.BackoffProperties backoff = new OutboxPublisherProperties.BackoffProperties();
        backoff.setEnabled(true);
        backoff.setDelay(Duration.ofSeconds(5));
        backoff.setMultiplier(2L);
        defaults.setBackoff(backoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.afterPropertiesSet();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertEquals(50, resultEvent.getBatchSize());
        assertEquals(Duration.ofSeconds(10), resultEvent.getInitialDelay());
        assertEquals(Duration.ofSeconds(60), resultEvent.getFixedDelay());
        assertEquals(5, resultEvent.getMaxRetries());
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(5), resultEvent.getBackoff().getDelay());
        assertEquals(2, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should override defaults with event specific values")
    public void initialize_whenEventPropertiesProvided_thenOverrideDefaults() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        defaults.setBatchSize(50);
        defaults.setInitialDelay(Duration.ofSeconds(10));
        defaults.setFixedDelay(Duration.ofSeconds(60));
        defaults.setMaxRetries(5);
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(5));
        defaultBackoff.setMultiplier(2L);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        event.setBatchSize(100);
        event.setInitialDelay(Duration.ofSeconds(20));
        event.setFixedDelay(Duration.ofSeconds(120));
        event.setMaxRetries(10);
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(15));
        eventBackoff.setMultiplier(5L);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.afterPropertiesSet();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertEquals(100, resultEvent.getBatchSize());
        assertEquals(Duration.ofSeconds(20), resultEvent.getInitialDelay());
        assertEquals(Duration.ofSeconds(120), resultEvent.getFixedDelay());
        assertEquals(10, resultEvent.getMaxRetries());
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(15), resultEvent.getBackoff().getDelay());
        assertEquals(5, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should handle disabled backoff in event properties")
    public void initialize_whenEventBackoffDisabled_thenApplyCorrectly() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        defaults.setBatchSize(50);
        defaults.setInitialDelay(Duration.ofSeconds(10));
        defaults.setFixedDelay(Duration.ofSeconds(60));
        defaults.setMaxRetries(5);
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(5));
        defaultBackoff.setMultiplier(2L);
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
        properties.afterPropertiesSet();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertFalse(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(0), resultEvent.getBackoff().getDelay());
        assertEquals(1, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should use default backoff delay when event backoff delay is null")
    public void initialize_whenEventBackoffDelayNull_thenUseDefaultDelay() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        defaults.setInitialDelay(Duration.ofSeconds(10));
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(3L);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setMultiplier(5L);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.afterPropertiesSet();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(10), resultEvent.getBackoff().getDelay());
        assertEquals(5, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should use event backoff delay when provided")
    public void initialize_whenEventBackoffDelayProvided_thenUseEventDelay() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(3L);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(25));
        eventBackoff.setMultiplier(5L);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.afterPropertiesSet();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(25), resultEvent.getBackoff().getDelay());
        assertEquals(5, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should use default backoff multiplier when event multiplier is null")
    public void initialize_whenEventBackoffMultiplierNull_thenUseDefaultMultiplier() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(4L);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(20));
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.afterPropertiesSet();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(20), resultEvent.getBackoff().getDelay());
        assertEquals(3, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should use default backoff multiplier when event multiplier is zero")
    public void initialize_whenEventBackoffMultiplierZero_thenUseDefaultMultiplier() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(4L);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(20));
        eventBackoff.setMultiplier(0L);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.afterPropertiesSet();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(20), resultEvent.getBackoff().getDelay());
        assertEquals(4L, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should use default backoff multiplier when event multiplier is negative")
    public void initialize_whenEventBackoffMultiplierNegative_thenUseDefaultMultiplier() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(4L);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(20));
        eventBackoff.setMultiplier(-2L);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.afterPropertiesSet();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(20), resultEvent.getBackoff().getDelay());
        assertEquals(4L, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should use event backoff multiplier when it is 1")
    public void initialize_whenEventBackoffMultiplierOne_thenUseEventMultiplier() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(4L);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(20));
        eventBackoff.setMultiplier(1L);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.afterPropertiesSet();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(20), resultEvent.getBackoff().getDelay());
        assertEquals(1, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should use event backoff multiplier when greater than 1")
    public void initialize_whenEventBackoffMultiplierGreaterThanOne_thenUseEventMultiplier() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(4L);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(20));
        eventBackoff.setMultiplier(7L);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.afterPropertiesSet();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(20), resultEvent.getBackoff().getDelay());
        assertEquals(7, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should use both defaults when event backoff delay and multiplier are null")
    public void initialize_whenEventBackoffDelayAndMultiplierNull_thenUseBothDefaults() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        defaults.setInitialDelay(Duration.ofSeconds(10));
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(4L);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.afterPropertiesSet();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(10), resultEvent.getBackoff().getDelay());
        assertEquals(3, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should use both event values when backoff delay and multiplier are provided")
    public void initialize_whenEventBackoffDelayAndMultiplierProvided_thenUseBothEventValues() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(4L);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(25));
        eventBackoff.setMultiplier(6L);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.afterPropertiesSet();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(25), resultEvent.getBackoff().getDelay());
        assertEquals(6, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should mix defaults and event values correctly")
    public void initialize_whenMixingDefaultsAndEventValues_thenApplyCorrectly() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(15));
        defaultBackoff.setMultiplier(4L);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties.BackoffProperties eventBackoff = new OutboxPublisherProperties.BackoffProperties();
        eventBackoff.setEnabled(true);
        eventBackoff.setDelay(Duration.ofSeconds(25));
        eventBackoff.setMultiplier(0L);
        event.setBackoff(eventBackoff);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.afterPropertiesSet();

        // then
        OutboxPublisherProperties.EventProperties resultEvent = properties.getEvents().get("test-event");
        assertTrue(resultEvent.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(25), resultEvent.getBackoff().getDelay());
        assertEquals(4L, resultEvent.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT isCleanUpEnabled() should return true when cleanUp is null")
    public void isCleanUpEnabled_whenCleanUpNull_thenReturnFalse() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        // when
        properties.afterPropertiesSet();

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
        properties.afterPropertiesSet();

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
        properties.afterPropertiesSet();

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

        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        cleanUp.setEnabled(true);
        cleanUp.setBatchSize(50);
        cleanUp.setTtl(Duration.ofHours(2));
        cleanUp.setInitialDelay(Duration.ofMinutes(5));
        cleanUp.setFixedDelay(Duration.ofMinutes(30));
        properties.setCleanUp(cleanUp);

        // when
        properties.afterPropertiesSet();

        // then
        assertNotNull(properties.getCleanUp());
        assertEquals(50, properties.getCleanUp().getBatchSize());
        assertEquals(Duration.ofHours(2), properties.getCleanUp().getTtl());
        assertEquals(Duration.ofMinutes(5), properties.getCleanUp().getInitialDelay());
        assertEquals(Duration.ofMinutes(30), properties.getCleanUp().getFixedDelay());
    }

    @Test
    @DisplayName("UT getDlq() should return dlq properties when provided")
    public void getDlq_whenDlqProvided_thenReturnProperties() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        OutboxPublisherProperties.DlqProperties dlq = new OutboxPublisherProperties.DlqProperties();
        dlq.setEnabled(true);
        dlq.setBatchSize(100);
        dlq.setTransferToInitialDelay(Duration.ofSeconds(60));
        dlq.setTransferToFixedDelay(Duration.ofSeconds(60));
        properties.setDlq(dlq);

        // when
        properties.afterPropertiesSet();

        // then
        assertNotNull(properties.getDlq());
        assertEquals(100, properties.getDlq().getBatchSize());
        assertEquals(Duration.ofSeconds(60), properties.getDlq().getTransferToInitialDelay());
        assertEquals(Duration.ofSeconds(60), properties.getDlq().getTransferToFixedDelay());
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
        event.setInitialDelay(Duration.ofSeconds(5));
        event.setFixedDelay(Duration.ofSeconds(30));
        event.setMaxRetries(3);
        events.put("test-event", event);
        properties.setEvents(events);

        // when
        properties.afterPropertiesSet();

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
        properties.afterPropertiesSet();

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
        properties.afterPropertiesSet();

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
        properties.afterPropertiesSet();

        // then
        assertNotNull(properties.getSender());
        assertEquals(SenderType.KAFKA, properties.getSender().getType());
        assertEquals("testBean", properties.getSender().getBeanName());
    }

    @Test
    @DisplayName("UT OutboxProperties.initialize() should handle multiple events with mixed configurations")
    public void initialize_withMultipleEvents_shouldApplyCorrectly() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);

        OutboxPublisherProperties.Defaults defaults = new OutboxPublisherProperties.Defaults();
        defaults.setBatchSize(100);
        defaults.setInitialDelay(Duration.ofSeconds(5));
        defaults.setFixedDelay(Duration.ofSeconds(30));
        defaults.setMaxRetries(3);
        OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
        defaultBackoff.setEnabled(true);
        defaultBackoff.setDelay(Duration.ofSeconds(10));
        defaultBackoff.setMultiplier(2L);
        defaults.setBackoff(defaultBackoff);
        properties.setDefaults(defaults);

        Map<String, OutboxPublisherProperties.EventProperties> events = new HashMap<>();

        OutboxPublisherProperties.EventProperties event1 = new OutboxPublisherProperties.EventProperties();
        event1.setTopic("topic1");
        events.put("event1", event1);

        OutboxPublisherProperties.EventProperties event2 = new OutboxPublisherProperties.EventProperties();
        event2.setTopic("topic2");
        event2.setBatchSize(50);
        event2.setFixedDelay(Duration.ofSeconds(60));
        OutboxPublisherProperties.BackoffProperties backoff2 = new OutboxPublisherProperties.BackoffProperties();
        backoff2.setEnabled(false);
        event2.setBackoff(backoff2);
        events.put("event2", event2);

        OutboxPublisherProperties.EventProperties event3 = new OutboxPublisherProperties.EventProperties();
        event3.setTopic("topic3");
        event3.setBatchSize(200);
        event3.setInitialDelay(Duration.ofSeconds(15));
        event3.setFixedDelay(Duration.ofSeconds(90));
        event3.setMaxRetries(5);
        OutboxPublisherProperties.BackoffProperties backoff3 = new OutboxPublisherProperties.BackoffProperties();
        backoff3.setEnabled(true);
        backoff3.setDelay(Duration.ofSeconds(20));
        backoff3.setMultiplier(3L);
        event3.setBackoff(backoff3);
        events.put("event3", event3);

        properties.setEvents(events);

        // when
        properties.afterPropertiesSet();

        // then
        OutboxPublisherProperties.EventProperties resultEvent1 = properties.getEvents().get("event1");
        assertEquals(100, resultEvent1.getBatchSize());
        assertEquals(Duration.ofSeconds(5), resultEvent1.getInitialDelay());
        assertEquals(Duration.ofSeconds(30), resultEvent1.getFixedDelay());
        assertEquals(3, resultEvent1.getMaxRetries());
        assertTrue(resultEvent1.getBackoff().isEnabled());

        OutboxPublisherProperties.EventProperties resultEvent2 = properties.getEvents().get("event2");
        assertEquals(50, resultEvent2.getBatchSize());
        assertEquals(Duration.ofSeconds(5), resultEvent2.getInitialDelay());
        assertEquals(Duration.ofSeconds(60), resultEvent2.getFixedDelay());
        assertEquals(3, resultEvent2.getMaxRetries());
        assertFalse(resultEvent2.getBackoff().isEnabled());

        OutboxPublisherProperties.EventProperties resultEvent3 = properties.getEvents().get("event3");
        assertEquals(200, resultEvent3.getBatchSize());
        assertEquals(Duration.ofSeconds(15), resultEvent3.getInitialDelay());
        assertEquals(Duration.ofSeconds(90), resultEvent3.getFixedDelay());
        assertEquals(5, resultEvent3.getMaxRetries());
        assertTrue(resultEvent3.getBackoff().isEnabled());
        assertEquals(Duration.ofSeconds(20), resultEvent3.getBackoff().getDelay());
        assertEquals(3, resultEvent3.getBackoff().getMultiplier());
    }

    @Test
    @DisplayName("UT setEnabled() should update enabled flag")
    public void setEnabled_shouldUpdateEnabledFlag() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();

        // when
        properties.setEnabled(true);

        // then
        assertTrue(properties.isEnabled());

        // when
        properties.setEnabled(false);

        // then
        assertFalse(properties.isEnabled());
    }

    @Test
    @DisplayName("UT setMetrics() should update metrics properties")
    public void setMetrics_shouldUpdateMetricsProperties() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.MetricsProperties metrics = new OutboxPublisherProperties.MetricsProperties();

        // when
        properties.setMetrics(metrics);

        // then
        assertEquals(metrics, properties.getMetrics());
    }

    @Test
    @DisplayName("UT afterPropertiesSet() when metrics is null should create default metrics")
    public void afterPropertiesSet_whenMetricsNull_shouldCreateDefaultMetrics() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(SenderType.KAFKA);
        sender.setBeanName("bean");
        properties.setSender(sender);
        properties.setEvents(new HashMap<>());

        // when
        properties.afterPropertiesSet();

        // then
        assertNotNull(properties.getMetrics());
        assertNotNull(properties.getMetrics().getGauge());
        // По умолчанию gauge.enabled = null -> false в afterPropertiesSet
        assertFalse(properties.getMetrics().getGauge().isEnabled());
    }

    @Test
    @DisplayName("UT afterPropertiesSet() when enabled is false should set enabled to false")
    public void afterPropertiesSet_whenEnabledFalse_shouldSetEnabledFalse() {
        // given
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        properties.setEnabled(false);

        // when
        properties.afterPropertiesSet();

        // then
        assertFalse(properties.isEnabled());
    }
}
