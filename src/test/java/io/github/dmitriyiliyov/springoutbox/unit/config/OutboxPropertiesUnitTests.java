package io.github.dmitriyiliyov.springoutbox.unit.config;

import io.github.dmitriyiliyov.springoutbox.config.OutboxProperties;
import io.github.dmitriyiliyov.springoutbox.config.SenderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class OutboxPropertiesUnitTests {

    @Test
    @DisplayName("UT OutboxProperties() should throw when sender is null")
    public void constructor_whenSenderNull_thenThrow() {
        // given + when + then
        assertThrows(NullPointerException.class,
                () -> new OutboxProperties(
                        null, null, null, null, null, null, null, null
                )
        );
    }

    @Test
    @DisplayName("UT OutboxProperties() should use default threadPoolSize when null")
    public void constructor_whenThreadPoolSizeNull_thenUseDefault() {
        // given + when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "beanName"),
                null, null, new HashMap<>(), null, null, null, null
        );

        // then
        assertEquals(Math.min(Runtime.getRuntime().availableProcessors(), 5), properties.getThreadPoolSize());
    }

    @Test
    @DisplayName("UT OutboxProperties() should use provided threadPoolSize")
    public void constructor_whenThreadPoolSizeProvided_thenUseIt() {
        // given + when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "beanName"),
                10, null, new HashMap<>(), null, null, null, null
        );

        // then
        assertEquals(10, properties.getThreadPoolSize());
    }

    @Test
    @DisplayName("UT OutboxProperties() should throw when events is null")
    public void constructor_whenEventsNull_thenThrow() {
        // given + when + then
        assertThrows(NullPointerException.class,
                () -> new OutboxProperties(
                        new OutboxProperties.SenderProperties(SenderType.KAFKA, "beanName"),
                        1, null, null, null, null, null, null
                )
        );
    }

    @Test
    @DisplayName("UT OutboxProperties() should accept empty events map")
    public void constructor_whenEventsEmpty_thenAccept() {
        // given + when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "beanName"),
                null, null, new HashMap<>(), null, null, null, null
        );

        // then
        assertTrue(properties.getEvents().isEmpty());
    }

    @Test
    @DisplayName("UT OutboxProperties() should throw when event type is null")
    public void constructor_whenEventTypeNull_thenThrow() {
        // given
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put(null, new OutboxProperties.EventProperties(
                "test", "topic", 10, Duration.ofSeconds(5), Duration.ofSeconds(30), 3, null
        ));

        // when + then
        assertThrows(NullPointerException.class,
                () -> new OutboxProperties(
                        new OutboxProperties.SenderProperties(SenderType.KAFKA, "beanName"),
                        null, null, events, null, null, null, null
                )
        );
    }

    @Test
    @DisplayName("UT OutboxProperties() should throw when event type is blank")
    public void constructor_whenEventTypeBlank_thenThrow() {
        // given
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("  ", new OutboxProperties.EventProperties(
                "test", "topic", 10, Duration.ofSeconds(5), Duration.ofSeconds(30), 3, null
        ));

        // when + then
        assertThrows(IllegalArgumentException.class,
                () -> new OutboxProperties(
                        new OutboxProperties.SenderProperties(SenderType.KAFKA, "beanName"),
                        null, null, events, null, null, null, null
                )
        );
    }

    @Test
    @DisplayName("UT OutboxProperties() should initialize stuckEventRecovery when null")
    public void constructor_whenStuckEventRecoveryNull_thenUseDefault() {
        // given + when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "beanName"),
                null, null, new HashMap<>(), null, null, null, null
        );

        // then
        assertEquals(new OutboxProperties.StuckEventRecoveryProperties(), properties.getStuckEventRecovery());
    }

    @Test
    @DisplayName("UT OutboxProperties() should initialize migration when null")
    public void constructor_whenMigrationNull_thenUseDefault() {
        // given + when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "beanName"),
                null, null, new HashMap<>(), null, null, null, null
        );

        // then
        assertEquals(new OutboxProperties.MigrationProperties(), properties.getMigration());
    }

    @Test
    @DisplayName("UT OutboxProperties() should initialize defaults when null")
    public void constructor_whenDefaultsNull_thenUseDefault() {
        // given + when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "beanName"),
                null, null, new HashMap<>(), null, null, null, null
        );

        // then
        assertEquals(new OutboxProperties.Defaults(), properties.getDefaults());
    }

    @Test
    @DisplayName("UT OutboxProperties() with full defaults should initialize all properties correctly")
    public void constructor_withDefaults_shouldInitializeAll() {
        // given
        OutboxProperties.SenderProperties sender = new OutboxProperties.SenderProperties(SenderType.KAFKA, "kafkaOutboxTemplate");
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("account-delete", new OutboxProperties.EventProperties(
                "account-delete", "account-delete", null, null, Duration.ofSeconds(10), 1, null
        ));
        events.put("user-registered", new OutboxProperties.EventProperties(
                "user-registered", "user-registered", null, null, null, 4, new OutboxProperties.BackoffProperties()
        ));
        OutboxProperties.StuckEventRecoveryProperties stuckRecovery = new OutboxProperties.StuckEventRecoveryProperties(null, null, null);
        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties(null, null, null, null, null);
        OutboxProperties.DlqProperties dlq = new OutboxProperties.DlqProperties(true, null, null, null, null, null);
        OutboxProperties.MigrationProperties migration = new OutboxProperties.MigrationProperties(null, null, null);
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(null, null, null, null, null);

        // when
        OutboxProperties properties = new OutboxProperties(sender, 5, defaults, events, stuckRecovery, cleanUp, dlq, migration);

        // then
        assertEquals(SenderType.KAFKA, sender.type());
        assertEquals("kafkaOutboxTemplate", sender.beanName());
        assertEquals(5, properties.getThreadPoolSize());

        OutboxProperties.Defaults resultDefaults = properties.getDefaults();
        OutboxProperties.Defaults defaultDefaults = new OutboxProperties.Defaults();
        assertEquals(defaultDefaults, resultDefaults);

        assertEquals(defaultDefaults.getBatchSize(), properties.getEvents().get("account-delete").batchSize());
        assertEquals(defaultDefaults.getInitialDelay(), properties.getEvents().get("account-delete").initialDelay());
        assertNotEquals(defaultDefaults.getFixedDelay(), properties.getEvents().get("account-delete").fixedDelay());
        assertNotEquals(defaultDefaults.getMaxRetries(), properties.getEvents().get("account-delete").maxRetries());
        assertEquals(defaultDefaults.getBackoff(), properties.getEvents().get("account-delete").backoff());

        assertEquals(defaultDefaults.getBatchSize(), properties.getEvents().get("user-registered").batchSize());
        assertEquals(defaultDefaults.getInitialDelay(), properties.getEvents().get("user-registered").initialDelay());
        assertEquals(defaultDefaults.getFixedDelay(), properties.getEvents().get("user-registered").fixedDelay());
        assertNotEquals(defaultDefaults.getMaxRetries(), properties.getEvents().get("user-registered").maxRetries());
        assertEquals(defaultDefaults.getBackoff(), properties.getEvents().get("user-registered").backoff());

        assertEquals(100, properties.getStuckEventRecovery().getBatchSize());
        assertEquals(Duration.ofSeconds(300), properties.getStuckEventRecovery().getInitialDelay());
        assertEquals(Duration.ofSeconds(1800), properties.getStuckEventRecovery().getFixedDelay());

        assertFalse(properties.isCleanUpEnabled());
        assertTrue(properties.getMigration().isEnabled());
        assertEquals("classpath:db/migration/outbox", properties.getMigration().getLocation());
        assertEquals("outbox_schema_history", properties.getMigration().getTable());
    }

    @Test
    @DisplayName("UT OutboxProperties() should handle Backoff in defaults correctly")
    public void constructor_whenBackoffDisabledInDefaults_thenApplyCorrectly() {
        // given
        OutboxProperties.SenderProperties sender = new OutboxProperties.SenderProperties(SenderType.KAFKA, "kafkaOutboxTemplate");
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("account-delete", new OutboxProperties.EventProperties(
                "account-delete", "account-delete", null, null, Duration.ofSeconds(10), 1, null
        ));
        events.put("user-registered", new OutboxProperties.EventProperties(
                "user-registered", "user-registered", null, null, null, 4, new OutboxProperties.BackoffProperties()
        ));
        OutboxProperties.StuckEventRecoveryProperties stuckRecovery = new OutboxProperties.StuckEventRecoveryProperties(null, null, null);
        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties(true, null, null, null, null);
        OutboxProperties.DlqProperties dlq = new OutboxProperties.DlqProperties(true, null, null, null, null, null);
        OutboxProperties.MigrationProperties migration = new OutboxProperties.MigrationProperties(null, null, null);
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(null, null, null, null,
                new OutboxProperties.BackoffProperties(false, null, null));

        // when
        OutboxProperties properties = new OutboxProperties(sender, 5, defaults, events, stuckRecovery, cleanUp, dlq, migration);

        // then
        OutboxProperties.Defaults resultDefaults = properties.getDefaults();
        OutboxProperties.Defaults defaultDefaults = new OutboxProperties.Defaults();
        assertNotEquals(defaultDefaults, resultDefaults);
        assertFalse(resultDefaults.getBackoff().isEnabled());

        assertFalse(properties.getEvents().get("account-delete").backoff().isEnabled());
        assertEquals(Duration.ofSeconds(0), properties.getEvents().get("account-delete").backoff().getDelay());
        assertEquals(1, properties.getEvents().get("account-delete").backoff().getMultiplier());

        assertTrue(properties.getEvents().get("user-registered").backoff().isEnabled());
        assertEquals(Duration.ofSeconds(10), properties.getEvents().get("user-registered").backoff().getDelay());
        assertEquals(3, properties.getEvents().get("user-registered").backoff().getMultiplier());

        assertEquals(100, properties.getStuckEventRecovery().getBatchSize());
        assertEquals(Duration.ofSeconds(300), properties.getStuckEventRecovery().getInitialDelay());
        assertEquals(Duration.ofSeconds(1800), properties.getStuckEventRecovery().getFixedDelay());

        assertTrue(properties.isCleanUpEnabled());
        OutboxProperties.CleanUpProperties cleanUpProperties = properties.getCleanUp().get();
        assertEquals(100, cleanUpProperties.batchSize());
        assertEquals(Duration.ofHours(1), cleanUpProperties.threshold());
        assertEquals(Duration.ofSeconds(300), cleanUpProperties.initialDelay());
        assertEquals(Duration.ofSeconds(5), cleanUpProperties.fixedDelay());

        assertTrue(properties.getMigration().isEnabled());
        assertEquals("classpath:db/migration/outbox", properties.getMigration().getLocation());
        assertEquals("outbox_schema_history", properties.getMigration().getTable());
    }

    @Test
    @DisplayName("UT OutboxProperties() should apply defaults to event properties with null values")
    public void constructor_whenEventPropertiesNull_thenApplyDefaults() {
        // given
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
                50, Duration.ofSeconds(10), Duration.ofSeconds(60), 5,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(5), 2L)
        );
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("test-event", new OutboxProperties.EventProperties(
                "test-event", "test-topic", null, null, null, null, null
        ));

        // when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, defaults, events, null, null, null, null
        );

        // then
        OutboxProperties.EventProperties event = properties.getEvents().get("test-event");
        assertEquals(50, event.batchSize());
        assertEquals(Duration.ofSeconds(10), event.initialDelay());
        assertEquals(Duration.ofSeconds(60), event.fixedDelay());
        assertEquals(5, event.maxRetries());
        assertTrue(event.backoff().isEnabled());
        assertEquals(Duration.ofSeconds(5), event.backoff().getDelay());
        assertEquals(2, event.backoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties() should override defaults with event specific values")
    public void constructor_whenEventPropertiesProvided_thenOverrideDefaults() {
        // given
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
                50, Duration.ofSeconds(10), Duration.ofSeconds(60), 5,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(5), 2L)
        );
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("test-event", new OutboxProperties.EventProperties(
                "test-event", "test-topic", 100, Duration.ofSeconds(20),
                Duration.ofSeconds(120), 10,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(15), 5L)
        ));

        // when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, defaults, events, null, null, null, null
        );

        // then
        OutboxProperties.EventProperties event = properties.getEvents().get("test-event");
        assertEquals(100, event.batchSize());
        assertEquals(Duration.ofSeconds(20), event.initialDelay());
        assertEquals(Duration.ofSeconds(120), event.fixedDelay());
        assertEquals(10, event.maxRetries());
        assertTrue(event.backoff().isEnabled());
        assertEquals(Duration.ofSeconds(15), event.backoff().getDelay());
        assertEquals(5, event.backoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties() should handle disabled backoff in event properties")
    public void constructor_whenEventBackoffDisabled_thenApplyCorrectly() {
        // given
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
                50, Duration.ofSeconds(10), Duration.ofSeconds(60), 5,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(5), 2L)
        );
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("test-event", new OutboxProperties.EventProperties(
                "test-event", "test-topic", null, null, null, null,
                new OutboxProperties.BackoffProperties(false, null, null)
        ));

        // when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, defaults, events, null, null, null, null
        );

        // then
        OutboxProperties.EventProperties event = properties.getEvents().get("test-event");
        assertFalse(event.backoff().isEnabled());
        assertEquals(Duration.ofSeconds(0), event.backoff().getDelay());
        assertEquals(1, event.backoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties() should handle backoff with invalid multiplier")
    public void constructor_whenBackoffMultiplierInvalid_thenUseDefault() {
        // given
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
                50, Duration.ofSeconds(10), Duration.ofSeconds(60), 5,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(5), 3L)
        );
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("test-event", new OutboxProperties.EventProperties(
                "test-event", "test-topic", null, null, null, null,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(15), 0L)
        ));

        // when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, defaults, events, null, null, null, null
        );

        // then
        OutboxProperties.EventProperties event = properties.getEvents().get("test-event");
        assertEquals(3, event.backoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties() should use default backoff delay when event backoff delay is null")
    public void constructor_whenEventBackoffDelayNull_thenUseDefaultDelay() {
        // given
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
                50, Duration.ofSeconds(10), Duration.ofSeconds(60), 5,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(15), 3L)
        );
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("test-event", new OutboxProperties.EventProperties(
                "test-event", "test-topic", null, null, null, null,
                new OutboxProperties.BackoffProperties(true, null, 5L)
        ));

        // when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, defaults, events, null, null, null, null
        );

        // then
        OutboxProperties.EventProperties event = properties.getEvents().get("test-event");
        assertTrue(event.backoff().isEnabled());
        assertEquals(Duration.ofSeconds(10), event.backoff().getDelay());
        assertEquals(5, event.backoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties() should use event backoff delay when provided")
    public void constructor_whenEventBackoffDelayProvided_thenUseEventDelay() {
        // given
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
                50, Duration.ofSeconds(10), Duration.ofSeconds(60), 5,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(15), 3L)
        );
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("test-event", new OutboxProperties.EventProperties(
                "test-event", "test-topic", null, null, null, null,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(25), 5L)
        ));

        // when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, defaults, events, null, null, null, null
        );

        // then
        OutboxProperties.EventProperties event = properties.getEvents().get("test-event");
        assertTrue(event.backoff().isEnabled());
        assertEquals(Duration.ofSeconds(25), event.backoff().getDelay());
        assertEquals(5, event.backoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties() should use default backoff multiplier when event multiplier is null")
    public void constructor_whenEventBackoffMultiplierNull_thenUseDefaultMultiplier() {
        // given
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
                50, Duration.ofSeconds(10), Duration.ofSeconds(60), 5,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(15), 4L)
        );
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("test-event", new OutboxProperties.EventProperties(
                "test-event", "test-topic", null, null, null, null,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(20), null)
        ));

        // when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, defaults, events, null, null, null, null
        );

        // then
        OutboxProperties.EventProperties event = properties.getEvents().get("test-event");
        assertTrue(event.backoff().isEnabled());
        assertEquals(Duration.ofSeconds(20), event.backoff().getDelay());
        assertEquals(3, event.backoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties() should use default backoff multiplier when event multiplier is zero")
    public void constructor_whenEventBackoffMultiplierZero_thenUseDefaultMultiplier() {
        // given
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
                50, Duration.ofSeconds(10), Duration.ofSeconds(60), 5,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(15), 4L)
        );
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("test-event", new OutboxProperties.EventProperties(
                "test-event", "test-topic", null, null, null, null,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(20), 0L)
        ));

        // when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, defaults, events, null, null, null, null
        );

        // then
        OutboxProperties.EventProperties event = properties.getEvents().get("test-event");
        assertTrue(event.backoff().isEnabled());
        assertEquals(Duration.ofSeconds(20), event.backoff().getDelay());
        assertEquals(3, event.backoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties() should use default backoff multiplier when event multiplier is negative")
    public void constructor_whenEventBackoffMultiplierNegative_thenUseDefaultMultiplier() {
        // given
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
                50, Duration.ofSeconds(10), Duration.ofSeconds(60), 5,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(15), 4L)
        );
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("test-event", new OutboxProperties.EventProperties(
                "test-event", "test-topic", null, null, null, null,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(20), -2L)
        ));

        // when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, defaults, events, null, null, null, null
        );

        // then
        OutboxProperties.EventProperties event = properties.getEvents().get("test-event");
        assertTrue(event.backoff().isEnabled());
        assertEquals(Duration.ofSeconds(20), event.backoff().getDelay());
        assertEquals(3, event.backoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties() should use event backoff multiplier when it is 1")
    public void constructor_whenEventBackoffMultiplierOne_thenUseEventMultiplier() {
        // given
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
                50, Duration.ofSeconds(10), Duration.ofSeconds(60), 5,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(15), 4L)
        );
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("test-event", new OutboxProperties.EventProperties(
                "test-event", "test-topic", null, null, null, null,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(20), 1L)
        ));

        // when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, defaults, events, null, null, null, null
        );

        // then
        OutboxProperties.EventProperties event = properties.getEvents().get("test-event");
        assertTrue(event.backoff().isEnabled());
        assertEquals(Duration.ofSeconds(20), event.backoff().getDelay());
        assertEquals(1, event.backoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties() should use event backoff multiplier when greater than 1")
    public void constructor_whenEventBackoffMultiplierGreaterThanOne_thenUseEventMultiplier() {
        // given
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
                50, Duration.ofSeconds(10), Duration.ofSeconds(60), 5,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(15), 4L)
        );
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("test-event", new OutboxProperties.EventProperties(
                "test-event", "test-topic", null, null, null, null,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(20), 7L)
        ));

        // when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, defaults, events, null, null, null, null
        );

        // then
        OutboxProperties.EventProperties event = properties.getEvents().get("test-event");
        assertTrue(event.backoff().isEnabled());
        assertEquals(Duration.ofSeconds(20), event.backoff().getDelay());
        assertEquals(7, event.backoff().getMultiplier());;
    }

    @Test
    @DisplayName("UT OutboxProperties() should use both defaults when event backoff delay and multiplier are null")
    public void constructor_whenEventBackoffDelayAndMultiplierNull_thenUseBothDefaults() {
        // given
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
                50, Duration.ofSeconds(10), Duration.ofSeconds(60), 5,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(15), 4L)
        );
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("test-event", new OutboxProperties.EventProperties(
                "test-event", "test-topic", null, null, null, null,
                new OutboxProperties.BackoffProperties(true, null, null)
        ));

        // when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, defaults, events, null, null, null, null
        );

        // then
        OutboxProperties.EventProperties event = properties.getEvents().get("test-event");
        assertTrue(event.backoff().isEnabled());
        assertEquals(Duration.ofSeconds(10), event.backoff().getDelay());
        assertEquals(3, event.backoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties() should use both event values when backoff delay and multiplier are provided")
    public void constructor_whenEventBackoffDelayAndMultiplierProvided_thenUseBothEventValues() {
        // given
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
                50, Duration.ofSeconds(10), Duration.ofSeconds(60), 5,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(15), 4L)
        );
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("test-event", new OutboxProperties.EventProperties(
                "test-event", "test-topic", null, null, null, null,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(25), 6L)
        ));

        // when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, defaults, events, null, null, null, null
        );

        // then
        OutboxProperties.EventProperties event = properties.getEvents().get("test-event");
        assertTrue(event.backoff().isEnabled());
        assertEquals(Duration.ofSeconds(25), event.backoff().getDelay());
        assertEquals(6, event.backoff().getMultiplier());
    }

    @Test
    @DisplayName("UT OutboxProperties() should mix defaults and event values correctly")
    public void constructor_whenMixingDefaultsAndEventValues_thenApplyCorrectly() {
        // given
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
                50, Duration.ofSeconds(10), Duration.ofSeconds(60), 5,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(15), 4L)
        );
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("test-event", new OutboxProperties.EventProperties(
                "test-event", "test-topic", null, null, null, null,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(25), 0L)
        ));

        // when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, defaults, events, null, null, null, null
        );

        // then
        OutboxProperties.EventProperties event = properties.getEvents().get("test-event");
        assertTrue(event.backoff().isEnabled());
        assertEquals(Duration.ofSeconds(25), event.backoff().getDelay());
        assertEquals(3, event.backoff().getMultiplier());
    }

    @Test
    @DisplayName("UT isCleanUpEnabled() should return false when cleanUp is null")
    public void isCleanUpEnabled_whenCleanUpNull_thenReturnFalse() {
        // given + when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, null, new HashMap<>(), null, null, null, null
        );

        // then
        assertFalse(properties.isCleanUpEnabled());
    }

    @Test
    @DisplayName("UT isCleanUpEnabled() should return true when cleanUp enabled")
    public void isCleanUpEnabled_whenCleanUpEnabled_thenReturnTrue() {
        // given + when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, null, new HashMap<>(), null,
                new OutboxProperties.CleanUpProperties(true, null, null, null, null),
                null, null
        );

        // then
        assertTrue(properties.isCleanUpEnabled());
    }

    @Test
    @DisplayName("UT isCleanUpEnabled() should return false when cleanUp disabled")
    public void isCleanUpEnabled_whenCleanUpDisabled_thenReturnFalse() {
        // given + when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, null, new HashMap<>(), null,
                new OutboxProperties.CleanUpProperties(false, null, null, null, null),
                null, null
        );

        // then
        assertFalse(properties.isCleanUpEnabled());
    }

    @Test
    @DisplayName("UT getCleanUp() should return Optional with cleanUp")
    public void getCleanUp_whenCleanUpProvided_thenReturnOptional() {
        // given
        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties(
                true, 50, Duration.ofHours(2), Duration.ofMinutes(5), Duration.ofMinutes(30)
        );

        // when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, null, new HashMap<>(), null, cleanUp, null, null
        );

        // then
        assertTrue(properties.getCleanUp().isPresent());
        assertEquals(cleanUp, properties.getCleanUp().get());
    }

    @Test
    @DisplayName("UT getDlq() should return Optional with dlq")
    public void getDlq_whenDlqProvided_thenReturnOptional() {
        // given
        OutboxProperties.DlqProperties dlq = new OutboxProperties.DlqProperties(
                true, 100, Duration.ofSeconds(60), Duration.ofSeconds(60), null, null
        );

        // when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, null, new HashMap<>(), null, null, dlq, null
        );

        // then
        assertTrue(properties.getDlq().isPresent());
        assertEquals(dlq, properties.getDlq().get());
    }

    @Test
    @DisplayName("UT existEventType() should return true when event exists")
    public void existEventType_whenEventExists_thenReturnTrue() {
        // given
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("test-event", new OutboxProperties.EventProperties(
                "test-event", "test-topic", 10, Duration.ofSeconds(5),
                Duration.ofSeconds(30), 3, null
        ));

        // when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, null, events, null, null, null, null
        );

        // then
        assertTrue(properties.existEventType("test-event"));
    }

    @Test
    @DisplayName("UT existEventType() should return false when event does not exist")
    public void existEventType_whenEventDoesNotExist_thenReturnFalse() {
        // given
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("test-event", new OutboxProperties.EventProperties(
                "test-event", "test-topic", 10, Duration.ofSeconds(5),
                Duration.ofSeconds(30), 3, null
        ));

        // when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, null, events, null, null, null, null
        );

        // then
        assertFalse(properties.existEventType("non-existing-event"));
    }

    @Test
    @DisplayName("UT getEvents() should return unmodifiable map")
    public void getEvents_shouldReturnUnmodifiableMap() {
        // given
        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("test-event", new OutboxProperties.EventProperties(
                "test-event", "test-topic", 10, Duration.ofSeconds(5),
                Duration.ofSeconds(30), 3, null
        ));

        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, null, events, null, null, null, null
        );

        // when + then
        assertThrows(UnsupportedOperationException.class,
                () -> properties.getEvents().put("new-event", new OutboxProperties.EventProperties(
                        "new-event", "topic", 1, Duration.ofSeconds(1),
                        Duration.ofSeconds(1), 1, null
                ))
        );
    }

    @Test
    @DisplayName("UT getSender() should return sender properties")
    public void getSender_shouldReturnSenderProperties() {
        // given
        OutboxProperties.SenderProperties sender = new OutboxProperties.SenderProperties(
                SenderType.KAFKA, "testBean"
        );

        // when
        OutboxProperties properties = new OutboxProperties(
                sender, null, null, new HashMap<>(), null, null, null, null
        );

        // then
        assertEquals(sender, properties.getSender());
        assertEquals(SenderType.KAFKA, properties.getSender().type());
        assertEquals("testBean", properties.getSender().beanName());
    }

    @Test
    @DisplayName("UT getMigration() should return migration properties")
    public void getMigration_shouldReturnMigrationProperties() {
        // given
        OutboxProperties.MigrationProperties migration = new OutboxProperties.MigrationProperties(
                true, "custom/location", "custom_table"
        );

        // when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, null, new HashMap<>(), null, null, null, migration
        );

        // then
        assertEquals(migration, properties.getMigration());
        assertTrue(properties.getMigration().isEnabled());
        assertEquals("custom/location", properties.getMigration().getLocation());
        assertEquals("custom_table", properties.getMigration().getTable());
    }

    @Test
    @DisplayName("UT OutboxProperties() should handle multiple events with mixed configurations")
    public void constructor_withMultipleEvents_shouldApplyCorrectly() {
        // given
        OutboxProperties.Defaults defaults = new OutboxProperties.Defaults(
                100, Duration.ofSeconds(5), Duration.ofSeconds(30), 3,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(10), 2L)
        );

        Map<String, OutboxProperties.EventProperties> events = new HashMap<>();
        events.put("event1", new OutboxProperties.EventProperties(
                "event1", "topic1", null, null, null, null, null
        ));
        events.put("event2", new OutboxProperties.EventProperties(
                "event2", "topic2", 50, null, Duration.ofSeconds(60), null,
                new OutboxProperties.BackoffProperties(false, null, null)
        ));
        events.put("event3", new OutboxProperties.EventProperties(
                "event3", "topic3", 200, Duration.ofSeconds(15),
                Duration.ofSeconds(90), 5,
                new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(20), 3L)
        ));

        // when
        OutboxProperties properties = new OutboxProperties(
                new OutboxProperties.SenderProperties(SenderType.KAFKA, "bean"),
                null, defaults, events, null, null, null, null
        );

        // then
        OutboxProperties.EventProperties event1 = properties.getEvents().get("event1");
        assertEquals(100, event1.batchSize());
        assertEquals(Duration.ofSeconds(5), event1.initialDelay());
        assertEquals(Duration.ofSeconds(30), event1.fixedDelay());
        assertEquals(3, event1.maxRetries());
        assertTrue(event1.backoff().isEnabled());

        OutboxProperties.EventProperties event2 = properties.getEvents().get("event2");
        assertEquals(50, event2.batchSize());
        assertEquals(Duration.ofSeconds(5), event2.initialDelay());
        assertEquals(Duration.ofSeconds(60), event2.fixedDelay());
        assertEquals(3, event2.maxRetries());
        assertFalse(event2.backoff().isEnabled());

        OutboxProperties.EventProperties event3 = properties.getEvents().get("event3");
        assertEquals(200, event3.batchSize());
        assertEquals(Duration.ofSeconds(15), event3.initialDelay());
        assertEquals(Duration.ofSeconds(90), event3.fixedDelay());
        assertEquals(5, event3.maxRetries());
        assertTrue(event3.backoff().isEnabled());
        assertEquals(Duration.ofSeconds(20), event3.backoff().getDelay());
        assertEquals(3, event3.backoff().getMultiplier());
    }
}