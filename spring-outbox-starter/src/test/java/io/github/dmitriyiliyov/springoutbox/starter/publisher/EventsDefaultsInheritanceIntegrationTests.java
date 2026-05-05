package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.starter.OutboxAutoConfiguration;
import io.github.dmitriyiliyov.springoutbox.starter.OutboxProperties;
import io.github.dmitriyiliyov.springoutbox.starter.PollingType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class EventsDefaultsInheritanceIntegrationTests {

    private ApplicationContextRunner getBaseContextRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        TransactionAutoConfiguration.class,
                        JacksonAutoConfiguration.class,
                        OutboxAutoConfiguration.class,
                        OutboxPublisherAutoConfiguration.class,
                        OutboxDlqAutoConfiguration.class
                ))
                .withBean(DataSource.class, () -> {
                    try {
                        DataSource mockDataSource = Mockito.mock(DataSource.class);
                        Connection mockConnection = Mockito.mock(Connection.class);
                        DatabaseMetaData mockMetaData = Mockito.mock(DatabaseMetaData.class);

                        Mockito.when(mockDataSource.getConnection()).thenReturn(mockConnection);
                        Mockito.when(mockConnection.getMetaData()).thenReturn(mockMetaData);
                        Mockito.when(mockMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");

                        return mockDataSource;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .withBean(KafkaTemplate.class, () -> {
                    KafkaTemplate<String, Object> mockTemplate = Mockito.mock(KafkaTemplate.class);
                    ProducerFactory<String, Object> mockFactory = Mockito.mock(ProducerFactory.class);

                    Mockito.when(mockTemplate.getProducerFactory()).thenReturn(mockFactory);
                    Mockito.when(mockFactory.getConfigurationProperties()).thenReturn(Collections.emptyMap());

                    return mockTemplate;
                })
                .withBean(TransactionTemplate.class, () -> Mockito.mock(TransactionTemplate.class))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withBean(Clock.class, Clock::systemDefaultZone)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:postgresql://outbox-producer-postgresql:5432/outbox_example",
                        "spring.datasource.driver-class-name=org.postgresql.Driver",
                        "spring.datasource.username=admin",
                        "spring.datasource.password=root",
                        "outbox.tables.auto-create=false",
                        "outbox.publisher.enabled=true",
                        "outbox.publisher.sender.type=kafka",
                        "outbox.publisher.events.my-event.topic=my.topic",
                        "outbox.publisher.dlq.enabled=true"
                );
    }

    private ApplicationContextRunner getPropertiesRunner() {
        return getBaseContextRunner().withPropertyValues(
                "outbox.publisher.defaults.batch-size=200",
                "outbox.publisher.defaults.polling.type=adaptive",
                "outbox.publisher.defaults.polling.initial-delay=5m",
                "outbox.publisher.defaults.polling.min-fixed-delay=1s",
                "outbox.publisher.defaults.polling.max-fixed-delay=1m",
                "outbox.publisher.defaults.polling.multiplier=1.5",
                "outbox.publisher.defaults.max-retries=3",
                "outbox.publisher.defaults.backoff.enabled=true",
                "outbox.publisher.defaults.backoff.delay=10s",
                "outbox.publisher.defaults.backoff.multiplier=3.0"
        );
    }

    @Test
    @DisplayName("IT High Priority should inherit defaults and override specific polling")
    void highPriority_inheritsAndOverrides() {
        getPropertiesRunner()
                .withPropertyValues(
                        "outbox.publisher.events.high-priority.topic=events",
                        "outbox.publisher.events.high-priority.polling.min-fixed-delay=250ms"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    OutboxProperties properties = context.getBean(OutboxProperties.class);
                    var highPriority = properties.getPublisher().getEvents().get("high-priority");

                    assertThat(highPriority).isNotNull();
                    assertThat(highPriority.getBatchSize()).isEqualTo(200);
                    assertThat(highPriority.getMaxRetries()).isEqualTo(3);
                    assertThat(highPriority.getPolling().getType()).isEqualTo(PollingType.ADAPTIVE);
                    assertThat(highPriority.getPolling().getMinFixedDelay()).isEqualTo(Duration.ofMillis(250));
                    assertThat(highPriority.getPolling().getMultiplier()).isEqualTo(1.5);
                    assertThat(highPriority.getPolling().getMaxFixedDelay()).isEqualTo(Duration.ofMinutes(1));
                });
    }

    @Test
    @DisplayName("IT Low Priority should disable backoff and override polling block")
    void lowPriority_overridesPollingAndBackoff() {
        getPropertiesRunner()
                .withPropertyValues(
                        "outbox.publisher.events.low-priority.topic=events",
                        "outbox.publisher.events.low-priority.polling.min-fixed-delay=10s",
                        "outbox.publisher.events.low-priority.polling.max-fixed-delay=5m",
                        "outbox.publisher.events.low-priority.polling.multiplier=2.0",
                        "outbox.publisher.events.low-priority.backoff.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    OutboxProperties properties = context.getBean(OutboxProperties.class);
                    var lowPriority = properties.getPublisher().getEvents().get("low-priority");

                    assertThat(lowPriority).isNotNull();
                    assertThat(lowPriority.getBackoff().isEnabled()).isFalse();
                    assertThat(lowPriority.getPolling().getType()).isEqualTo(PollingType.ADAPTIVE);
                    assertThat(lowPriority.getPolling().getMinFixedDelay()).isEqualTo(Duration.ofSeconds(10));
                    assertThat(lowPriority.getPolling().getMaxFixedDelay()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(lowPriority.getPolling().getMultiplier()).isEqualTo(2.0);
                    assertThat(lowPriority.getBatchSize()).isEqualTo(200);
                });
    }

    @Test
    @DisplayName("IT Default Priority should inherit exactly all defaults without overrides")
    void defaultPriority_inheritsEverything() {
        getPropertiesRunner()
                .withPropertyValues(
                        "outbox.publisher.events.default-priority.topic=events"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    OutboxProperties properties = context.getBean(OutboxProperties.class);
                    var defaultPriority = properties.getPublisher().getEvents().get("default-priority");

                    assertThat(defaultPriority).isNotNull();
                    assertThat(defaultPriority.getBatchSize()).isEqualTo(200);
                    assertThat(defaultPriority.getMaxRetries()).isEqualTo(3);

                    assertThat(defaultPriority.getPolling().getType()).isEqualTo(PollingType.ADAPTIVE);
                    assertThat(defaultPriority.getPolling().getInitialDelay()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(defaultPriority.getPolling().getMinFixedDelay()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(defaultPriority.getPolling().getMaxFixedDelay()).isEqualTo(Duration.ofMinutes(1));
                    assertThat(defaultPriority.getPolling().getMultiplier()).isEqualTo(1.5);

                    assertThat(defaultPriority.getBackoff().isEnabled()).isTrue();
                    assertThat(defaultPriority.getBackoff().getDelay()).isEqualTo(Duration.ofSeconds(10));
                    assertThat(defaultPriority.getBackoff().getMultiplier()).isEqualTo(3.0);
                });
    }

    @Test
    @DisplayName("IT Fixed Priority should switch type to FIXED and apply specific properties")
    void fixedPriority_switchesToFixedPolling() {
        getPropertiesRunner()
                .withPropertyValues(
                        "outbox.publisher.events.fixed-priority.topic=events",
                        "outbox.publisher.events.fixed-priority.polling.type=fixed",
                        "outbox.publisher.events.fixed-priority.polling.fixed-delay=2s"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    OutboxProperties properties = context.getBean(OutboxProperties.class);
                    var fixedPriority = properties.getPublisher().getEvents().get("fixed-priority");

                    assertThat(fixedPriority).isNotNull();
                    assertThat(fixedPriority.getBatchSize()).isEqualTo(200);

                    assertThat(fixedPriority.getPolling().getType()).isEqualTo(PollingType.FIXED);
                    assertThat(fixedPriority.getPolling().getInitialDelay()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(fixedPriority.getPolling().getFixedDelay()).isEqualTo(Duration.ofSeconds(2));

                    assertThat(fixedPriority.getPolling().getMinFixedDelay()).isZero();
                    assertThat(fixedPriority.getPolling().getMaxFixedDelay()).isZero();
                    assertThat(fixedPriority.getPolling().getMultiplier()).isNaN();
                });
    }

    @Test
    @DisplayName("IT Complete Override should ignore defaults and apply all event specific values")
    void completeOverride_overridesAllDefaults() {
        getPropertiesRunner()
                .withPropertyValues(
                        "outbox.publisher.events.full-override.topic=custom-topic",
                        "outbox.publisher.events.full-override.batch-size=500",
                        "outbox.publisher.events.full-override.max-retries=10",
                        "outbox.publisher.events.full-override.polling.type=adaptive",
                        "outbox.publisher.events.full-override.polling.initial-delay=1s",
                        "outbox.publisher.events.full-override.polling.min-fixed-delay=100ms",
                        "outbox.publisher.events.full-override.polling.max-fixed-delay=200ms",
                        "outbox.publisher.events.full-override.polling.multiplier=5.0",
                        "outbox.publisher.events.full-override.backoff.enabled=true",
                        "outbox.publisher.events.full-override.backoff.delay=2s",
                        "outbox.publisher.events.full-override.backoff.multiplier=4.0"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    OutboxProperties properties = context.getBean(OutboxProperties.class);
                    var fullOverride = properties.getPublisher().getEvents().get("full-override");

                    assertThat(fullOverride).isNotNull();
                    assertThat(fullOverride.getTopic()).isEqualTo("custom-topic");
                    assertThat(fullOverride.getBatchSize()).isEqualTo(500);
                    assertThat(fullOverride.getMaxRetries()).isEqualTo(10);

                    assertThat(fullOverride.getPolling().getType()).isEqualTo(PollingType.ADAPTIVE);
                    assertThat(fullOverride.getPolling().getInitialDelay()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(fullOverride.getPolling().getMinFixedDelay()).isEqualTo(Duration.ofMillis(100));
                    assertThat(fullOverride.getPolling().getMaxFixedDelay()).isEqualTo(Duration.ofMillis(200));
                    assertThat(fullOverride.getPolling().getMultiplier()).isEqualTo(5.0);

                    assertThat(fullOverride.getBackoff().isEnabled()).isTrue();
                    assertThat(fullOverride.getBackoff().getDelay()).isEqualTo(Duration.ofSeconds(2));
                    assertThat(fullOverride.getBackoff().getMultiplier()).isEqualTo(4.0);
                });
    }

    @Test
    @DisplayName("IT Partial Backoff Override should inherit enabled state but apply specific delay and multiplier")
    void partialBackoffOverride_inheritsEnabledState() {
        getPropertiesRunner()
                .withPropertyValues(
                        "outbox.publisher.events.custom-backoff.topic=events",
                        "outbox.publisher.events.custom-backoff.backoff.delay=15s",
                        "outbox.publisher.events.custom-backoff.backoff.multiplier=2.5"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    OutboxProperties properties = context.getBean(OutboxProperties.class);
                    var customBackoff = properties.getPublisher().getEvents().get("custom-backoff");

                    assertThat(customBackoff).isNotNull();
                    assertThat(customBackoff.getBackoff().isEnabled()).isTrue();
                    assertThat(customBackoff.getBackoff().getDelay()).isEqualTo(Duration.ofSeconds(15));
                    assertThat(customBackoff.getBackoff().getMultiplier()).isEqualTo(2.5);
                });
    }

    @Test
    @DisplayName("IT Initial Delay Override should apply specific initial delay and inherit the rest of the polling configuration")
    void overrideInitialDelayOnly_inheritsRestOfPolling() {
        getPropertiesRunner()
                .withPropertyValues(
                        "outbox.publisher.events.fast-start.topic=events",
                        "outbox.publisher.events.fast-start.polling.initial-delay=10s"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    OutboxProperties properties = context.getBean(OutboxProperties.class);
                    var fastStart = properties.getPublisher().getEvents().get("fast-start");

                    assertThat(fastStart).isNotNull();
                    assertThat(fastStart.getPolling().getType()).isEqualTo(PollingType.ADAPTIVE);
                    assertThat(fastStart.getPolling().getInitialDelay()).isEqualTo(Duration.ofSeconds(10));
                    assertThat(fastStart.getPolling().getMinFixedDelay()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(fastStart.getPolling().getMaxFixedDelay()).isEqualTo(Duration.ofMinutes(1));
                    assertThat(fastStart.getPolling().getMultiplier()).isEqualTo(1.5);
                });
    }
}