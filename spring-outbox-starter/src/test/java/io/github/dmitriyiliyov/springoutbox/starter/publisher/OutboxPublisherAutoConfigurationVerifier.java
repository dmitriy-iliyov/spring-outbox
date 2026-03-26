package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.aop.OutboxPublishAspect;
import io.github.dmitriyiliyov.springoutbox.aop.RowOutboxEventListener;
import io.github.dmitriyiliyov.springoutbox.core.publisher.*;
import io.github.dmitriyiliyov.springoutbox.core.publisher.utils.UuidGenerator;
import io.github.dmitriyiliyov.springoutbox.starter.OutboxAutoConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class OutboxPublisherAutoConfigurationVerifier {

    private final String dbUrl;
    private final String dbDriver;
    private final String dbUsername;
    private final String dbPassword;

    public OutboxPublisherAutoConfigurationVerifier(String dbUrl, String dbDriver, String dbUsername, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbDriver = dbDriver;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    private ApplicationContextRunner getBaseContextRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        DataSourceTransactionManagerAutoConfiguration.class,
                        TransactionAutoConfiguration.class,
                        JacksonAutoConfiguration.class,
                        OutboxAutoConfiguration.class,
                        OutboxPublisherAutoConfiguration.class,
                        KafkaAutoConfiguration.class
                ))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues(
                        "spring.datasource.url=" + dbUrl,
                        "spring.datasource.driver-class-name=" + dbDriver,
                        "spring.datasource.username=" + dbUsername,
                        "spring.datasource.password=" + dbPassword,
                        "outbox.tables.auto-create=false",
                        "outbox.publisher.enabled=true",
                        "outbox.publisher.sender.type=kafka",
                        "outbox.publisher.events.my-event.topic=my.topic"
                );
    }

    public void shouldNotLoadWhenDisabled() {
        getBaseContextRunner()
                .withPropertyValues("outbox.publisher.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(OutboxProcessor.class);
                    assertThat(ctx).doesNotHaveBean(OutboxManager.class);
                    assertThat(ctx).doesNotHaveBean(OutboxSender.class);
                });
    }

    public void shouldRegisterOutboxRepository() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxRepository.class)
        );
    }

    public void shouldRegisterOutboxManager() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxManager.class)
        );
    }

    public void shouldRegisterOutboxProcessor() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxProcessor.class)
        );
    }

    public void shouldRegisterOutboxSerializer() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxSerializer.class)
        );
    }

    public void shouldRegisterOutboxPublisher() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxPublisher.class)
        );
    }

    public void shouldRegisterUuidGenerator() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(UuidGenerator.class)
        );
    }

    public void shouldRegisterOutboxPublishAspect() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxPublishAspect.class)
        );
    }

    public void shouldRegisterRowOutboxEventListener() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(RowOutboxEventListener.class)
        );
    }

    public void shouldNotRegisterOutboxRepositoryWhenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customOutboxRepository", OutboxRepository.class, () -> org.mockito.Mockito.mock(OutboxRepository.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxRepository.class);
                    assertThat(ctx).hasBean("customOutboxRepository");
                });
    }

    public void shouldNotRegisterOutboxProcessorWhenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customOutboxProcessor", OutboxProcessor.class, () -> org.mockito.Mockito.mock(OutboxProcessor.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxProcessor.class);
                    assertThat(ctx).hasBean("customOutboxProcessor");
                });
    }
}