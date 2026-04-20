package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.metrics.publisher.dlq.OutboxDlqWebManagerMetricsDecorator;
import io.github.dmitriyiliyov.springoutbox.web.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OutboxDlqWebAutoConfigurationIntegrationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OutboxDlqWebAutoConfiguration.class))
            .withUserConfiguration(DependenciesConfiguration.class);

    @Test
    @DisplayName("IT should not register any beans when dlq.enabled property is missing")
    void shouldNotRegisterBeansWhenPropertyIsMissing() {
        contextRunner.run(context -> assertNoBeans(context));
    }

    @Test
    @DisplayName("IT should not register any beans when dlq.enabled=false")
    void shouldNotRegisterBeansWhenPropertyIsFalse() {
        contextRunner
                .withPropertyValues("outbox.publisher.dlq.enabled=false")
                .run(context -> assertNoBeans(context));
    }

    @Test
    @DisplayName("IT should register all beans when dlq.enabled=true and metrics disabled")
    void shouldRegisterAllBeansWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "outbox.publisher.dlq.enabled=true",
                        "outbox.publisher.dlq.metrics.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(OutboxDlqWebRepository.class);
                    assertThat(context).hasSingleBean(OutboxDlqController.class);
                    assertThat(context).hasSingleBean(DlqStatusQueryConverter.class);
                    assertThat(context).hasSingleBean(OutboxDlqControllerAdvice.class);

                    assertThat(context.getBean(OutboxDlqWebManager.class))
                            .isExactlyInstanceOf(DefaultOutboxDlqWebManager.class);
                    assertThat(context).doesNotHaveBean(OutboxDlqWebManagerMetricsDecorator.class);
                });
    }

    @Test
    @DisplayName("IT should register metrics decorator as primary when metrics.enabled=true")
    void shouldRegisterMetricsDecoratorWhenMetricsEnabled() {
        contextRunner
                .withPropertyValues(
                        "outbox.publisher.dlq.enabled=true",
                        "outbox.publisher.dlq.metrics.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasBean("outboxDlqWebManager");
                    assertThat(context).hasBean("outboxDlqWebManagerMetricsDecorator");
                    assertThat(context.getBean(OutboxDlqWebManager.class))
                            .isInstanceOf(OutboxDlqWebManagerMetricsDecorator.class);
                });
    }

    @Test
    @DisplayName("IT should not register metrics decorator when metrics.enabled is missing")
    void shouldNotRegisterMetricsDecoratorWhenMetricsPropertyMissing() {
        contextRunner
                .withPropertyValues("outbox.publisher.dlq.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OutboxDlqWebManagerMetricsDecorator.class);
                    assertThat(context.getBean(OutboxDlqWebManager.class))
                            .isExactlyInstanceOf(DefaultOutboxDlqWebManager.class);
                });
    }

    @Test
    @DisplayName("IT should not override custom OutboxDlqWebRepository")
    void shouldNotOverrideCustomRepository() {
        contextRunner
                .withPropertyValues("outbox.publisher.dlq.enabled=true")
                .withUserConfiguration(CustomRepositoryConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(OutboxDlqWebRepository.class);
                    assertThat(context.getBean(OutboxDlqWebRepository.class))
                            .isSameAs(context.getBean("customRepository"));
                });
    }

    @Test
    @DisplayName("IT should not override custom OutboxDlqWebManager")
    void shouldNotOverrideCustomManager() {
        contextRunner
                .withPropertyValues("outbox.publisher.dlq.enabled=true")
                .withUserConfiguration(CustomManagerConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(OutboxDlqWebManager.class);
                    assertThat(context.getBean(OutboxDlqWebManager.class))
                            .isSameAs(context.getBean("customManager"));
                });
    }

    @Test
    @DisplayName("IT should not override custom OutboxDlqController")
    void shouldNotOverrideCustomController() {
        contextRunner
                .withPropertyValues("outbox.publisher.dlq.enabled=true")
                .withUserConfiguration(CustomControllerConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(OutboxDlqController.class);
                    assertThat(context.getBean(OutboxDlqController.class))
                            .isSameAs(context.getBean("customController"));
                });
    }

    @Test
    @DisplayName("IT should not override custom DlqStatusQueryConverter")
    void shouldNotOverrideCustomConverter() {
        contextRunner
                .withPropertyValues("outbox.publisher.dlq.enabled=true")
                .withUserConfiguration(CustomConverterConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(DlqStatusQueryConverter.class);
                    assertThat(context.getBean(DlqStatusQueryConverter.class))
                            .isSameAs(context.getBean("customConverter"));
                });
    }

    @Test
    @DisplayName("IT should not override custom OutboxDlqControllerAdvice")
    void shouldNotOverrideCustomAdvice() {
        contextRunner
                .withPropertyValues("outbox.publisher.dlq.enabled=true")
                .withUserConfiguration(CustomAdviceConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(OutboxDlqControllerAdvice.class);
                    assertThat(context.getBean(OutboxDlqControllerAdvice.class))
                            .isSameAs(context.getBean("customAdvice"));
                });
    }

    private void assertNoBeans(AssertableApplicationContext context) {
        assertThat(context).doesNotHaveBean(OutboxDlqWebRepository.class);
        assertThat(context).doesNotHaveBean(OutboxDlqWebManager.class);
        assertThat(context).doesNotHaveBean(OutboxDlqController.class);
        assertThat(context).doesNotHaveBean(DlqStatusQueryConverter.class);
        assertThat(context).doesNotHaveBean(OutboxDlqControllerAdvice.class);
    }

    @Configuration
    static class DependenciesConfiguration {

        @Bean
        DataSource dataSource() throws SQLException {
            DataSource dataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);
            when(dataSource.getConnection().getMetaData().getDatabaseProductName()).thenReturn("PostgreSQL");
            return dataSource;
        }

        @Bean
        JdbcTemplate jdbcTemplate() {
            return mock(JdbcTemplate.class);
        }

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Configuration
    static class CustomRepositoryConfiguration {
        @Bean
        OutboxDlqWebRepository customRepository() {
            return mock(OutboxDlqWebRepository.class);
        }
    }

    @Configuration
    static class CustomManagerConfiguration {
        @Bean
        OutboxDlqWebManager customManager() {
            return mock(OutboxDlqWebManager.class);
        }
    }

    @Configuration
    static class CustomControllerConfiguration {
        @Bean
        OutboxDlqController customController() {
            return mock(OutboxDlqController.class);
        }
    }

    @Configuration
    static class CustomConverterConfiguration {
        @Bean
        DlqStatusQueryConverter customConverter() {
            return mock(DlqStatusQueryConverter.class);
        }
    }

    @Configuration
    static class CustomAdviceConfiguration {
        @Bean
        OutboxDlqControllerAdvice customAdvice() {
            return mock(OutboxDlqControllerAdvice.class);
        }
    }
}