package io.github.dmitriyiliyov.oncebox.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class OnAnyCleanUpEnabledConditionIntegrationTests {

    @Configuration
    static class TestConfiguration {

        @Bean
        @Conditional(OnAnyCleanUpEnabledCondition.class)
        String conditionalBean() {
            return "clean-up-enabled-bean";
        }
    }

    private ApplicationContextRunner baseRunner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(TestConfiguration.class);
    }

    @Test
    @DisplayName("IT should match when only publisher clean-up enabled")
    void shouldMatch_whenOnlyPublisherCleanUpEnabled() {
        baseRunner()
                .withPropertyValues("oncebox.publisher.clean-up.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should match when only consumer clean-up enabled")
    void shouldMatch_whenOnlyConsumerCleanUpEnabled() {
        baseRunner()
                .withPropertyValues("oncebox.consumer.clean-up.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should match when both publisher and consumer clean-up enabled")
    void shouldMatch_whenBothCleanUpEnabled() {
        baseRunner()
                .withPropertyValues(
                        "oncebox.publisher.clean-up.enabled=true",
                        "oncebox.consumer.clean-up.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should match when publisher clean-up enabled and consumer explicitly disabled")
    void shouldMatch_whenPublisherEnabledAndConsumerDisabled() {
        baseRunner()
                .withPropertyValues(
                        "oncebox.publisher.clean-up.enabled=true",
                        "oncebox.consumer.clean-up.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should match when consumer clean-up enabled and publisher explicitly disabled")
    void shouldMatch_whenConsumerEnabledAndPublisherDisabled() {
        baseRunner()
                .withPropertyValues(
                        "oncebox.publisher.clean-up.enabled=false",
                        "oncebox.consumer.clean-up.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should not match when both clean-up explicitly disabled")
    void shouldNotMatch_whenBothCleanUpDisabled() {
        baseRunner()
                .withPropertyValues(
                        "oncebox.publisher.clean-up.enabled=false",
                        "oncebox.consumer.clean-up.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should match when no clean-up properties specified")
    void shouldNotMatch_whenNoPropertiesSpecified() {
        baseRunner()
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should match when publisher absent and consumer explicitly disabled")
    void shouldNotMatch_whenPublisherAbsentAndConsumerDisabled() {
        baseRunner()
                .withPropertyValues("oncebox.consumer.clean-up.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should not match when consumer absent and publisher explicitly disabled")
    void shouldNotMatch_whenConsumerAbsentAndPublisherDisabled() {
        baseRunner()
                .withPropertyValues("oncebox.publisher.clean-up.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("conditionalBean");
                });
    }
}