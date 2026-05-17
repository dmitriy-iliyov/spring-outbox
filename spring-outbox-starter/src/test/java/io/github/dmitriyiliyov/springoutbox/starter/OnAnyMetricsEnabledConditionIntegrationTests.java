package io.github.dmitriyiliyov.springoutbox.starter;

import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxPublisherAutoConfiguration;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.dlq.OutboxDlqAutoConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.Clock;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OnAnyMetricsEnabledConditionIntegrationTests {

    @Configuration
    static class TestConfiguration {

        @Bean
        @Conditional(OnAnyMetricsEnabledCondition.class)
        String conditionalBean() {
            return "metrics-enabled-bean";
        }
    }

    private ApplicationContextRunner baseRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        TransactionAutoConfiguration.class,
                        JacksonAutoConfiguration.class,
                        OutboxAutoConfiguration.class,
                        OutboxPublisherAutoConfiguration.class,
                        OutboxDlqAutoConfiguration.class
                ))
                .withUserConfiguration(TestConfiguration.class)
                .withBean(DataSource.class, () -> {
                    try {
                        DataSource mockDataSource = mock(DataSource.class);
                        Connection mockConnection = mock(Connection.class);
                        DatabaseMetaData mockMetaData = mock(DatabaseMetaData.class);
                        Mockito.when(mockDataSource.getConnection()).thenReturn(mockConnection);
                        Mockito.when(mockConnection.getMetaData()).thenReturn(mockMetaData);
                        Mockito.when(mockMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
                        return mockDataSource;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .withBean(KafkaTemplate.class, () -> {
                    KafkaTemplate<String, Object> mockTemplate = mock(KafkaTemplate.class);
                    ProducerFactory<String, Object> mockFactory = mock(ProducerFactory.class);
                    Mockito.when(mockTemplate.getProducerFactory()).thenReturn(mockFactory);
                    Mockito.when(mockFactory.getConfigurationProperties()).thenReturn(Collections.emptyMap());
                    return mockTemplate;
                })
                .withBean(TransactionTemplate.class, () -> mock(TransactionTemplate.class))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withBean(Clock.class, Clock::systemDefaultZone)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:postgresql://outbox-producer-postgresql:5432/outbox_example",
                        "spring.datasource.driver-class-name=org.postgresql.Driver",
                        "spring.datasource.username=admin",
                        "spring.datasource.password=root",
                        "outbox.tables.auto-create=false",
                        "outbox.publisher.sender.type=kafka",
                        "outbox.publisher.events.my-event.topic=my.topic"
                );
    }

    @Test
    @DisplayName("IT should match when only publisher metrics enabled")
    void shouldMatch_whenOnlyPublisherMetricsEnabled() {
        baseRunner()
                .withPropertyValues("outbox.publisher.metrics.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should match when only consumer metrics enabled")
    void shouldMatch_whenOnlyConsumerMetricsEnabled() {
        baseRunner()
                .withPropertyValues("outbox.consumer.metrics.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should match when both publisher and consumer metrics enabled")
    void shouldMatch_whenBothMetricsEnabled() {
        baseRunner()
                .withPropertyValues(
                        "outbox.publisher.metrics.enabled=true",
                        "outbox.consumer.metrics.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should match when publisher enabled and consumer explicitly disabled")
    void shouldMatch_whenPublisherEnabledAndConsumerDisabled() {
        baseRunner()
                .withPropertyValues(
                        "outbox.publisher.metrics.enabled=true",
                        "outbox.consumer.metrics.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should match when consumer enabled and publisher explicitly disabled")
    void shouldMatch_whenConsumerEnabledAndPublisherDisabled() {
        baseRunner()
                .withPropertyValues(
                        "outbox.publisher.metrics.enabled=false",
                        "outbox.consumer.metrics.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should not match when both metrics explicitly disabled")
    void shouldNotMatch_whenBothMetricsDisabled() {
        baseRunner()
                .withPropertyValues(
                        "outbox.publisher.metrics.enabled=false",
                        "outbox.consumer.metrics.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should not match when no metrics properties specified")
    void shouldNotMatch_whenNoPropertiesSpecified() {
        baseRunner()
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should not match when publisher absent and consumer explicitly disabled")
    void shouldNotMatch_whenPublisherAbsentAndConsumerDisabled() {
        baseRunner()
                .withPropertyValues("outbox.consumer.metrics.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should not match when consumer absent and publisher explicitly disabled")
    void shouldNotMatch_whenConsumerAbsentAndPublisherDisabled() {
        baseRunner()
                .withPropertyValues("outbox.publisher.metrics.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("conditionalBean");
                });
    }
}