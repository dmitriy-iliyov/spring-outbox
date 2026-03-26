package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxManager;
import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxEventIdResolveManager;
import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.springoutbox.starter.OutboxAutoConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class OutboxConsumerAutoConfigurationVerifier {

    private final String dbUrl;
    private final String dbDriver;
    private final String dbPassword;
    private final String dbUsername;

    public OutboxConsumerAutoConfigurationVerifier(String dbUrl, String dbDriver, String dbPassword, String dbUsername) {
        this.dbUrl = dbUrl;
        this.dbDriver = dbDriver;
        this.dbPassword = dbPassword;
        this.dbUsername = dbUsername;
    }

    private ApplicationContextRunner getBaseContextRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        DataSourceTransactionManagerAutoConfiguration.class,
                        TransactionAutoConfiguration.class,
                        CacheAutoConfiguration.class,
                        OutboxAutoConfiguration.class,
                        OutboxConsumerAutoConfiguration.class
                ))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withBean(CacheManager.class, () -> new ConcurrentMapCacheManager("outbox"))
                .withPropertyValues(
                        "spring.datasource.url=" + dbUrl,
                        "spring.datasource.driver-class-name=" + dbDriver,
                        "spring.datasource.username=" + dbUsername,
                        "spring.datasource.password=" + dbPassword,
                        "outbox.tables.auto-create=false",
                        "outbox.consumer.enabled=true",
                        "outbox.consumer.cache.cache-name=outbox"
                );
    }

    public void shouldNotLoadWhenDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        DataSourceTransactionManagerAutoConfiguration.class,
                        TransactionAutoConfiguration.class,
                        CacheAutoConfiguration.class,
                        OutboxAutoConfiguration.class,
                        OutboxConsumerAutoConfiguration.class
                ))
                .withPropertyValues(
                        "spring.datasource.url=" + dbUrl,
                        "spring.datasource.driver-class-name=" + dbDriver,
                        "spring.datasource.username=" + dbUsername,
                        "spring.datasource.password=" + dbPassword,
                        "outbox.tables.auto-create=false",
                        "outbox.consumer.enabled=false"
                )
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(OutboxIdempotentConsumer.class);
                    assertThat(ctx).doesNotHaveBean(ConsumedOutboxManager.class);
                    assertThat(ctx).doesNotHaveBean(ConsumedOutboxRepository.class);
                });
    }

    public void shouldRegisterConsumedOutboxRepository() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(ConsumedOutboxRepository.class)
        );
    }

    public void shouldRegisterConsumedOutboxManager() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(ConsumedOutboxManager.class)
        );
    }

    public void shouldRegisterOutboxIdempotentConsumer() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxIdempotentConsumer.class)
        );
    }

    public void shouldRegisterOutboxEventIdResolveManager() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxEventIdResolveManager.class)
        );
    }

    public void shouldRegisterKafkaEventIdResolver() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasBean("kafkaOutboxEventIdResolver")
        );
    }

    public void shouldRegisterRabbitMqEventIdResolver() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasBean("rabbitMqOutboxEventIdResolver")
        );
    }

    public void shouldNotRegisterCleanUpSchedulerWhenDisabled() {
        getBaseContextRunner()
                .withPropertyValues("outbox.consumer.clean-up.enabled=false")
                .run(ctx ->
                        assertThat(ctx).doesNotHaveBean("consumedOutboxCleanUpScheduler")
                );
    }

    public void shouldNotRegisterDuplicateConsumedOutboxManagerWhenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean(
                        "customConsumedOutboxManager",
                        ConsumedOutboxManager.class,
                        () -> org.mockito.Mockito.mock(ConsumedOutboxManager.class)
                )
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ConsumedOutboxManager.class);
                    assertThat(ctx).hasBean("customConsumedOutboxManager");
                });
    }

    public void shouldCreateTablesWhenAutoCreateTrue() {
        getBaseContextRunner()
                .withPropertyValues("outbox.tables.auto-create=true")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    JdbcTemplate jdbcTemplate = ctx.getBean("outboxTransactionAwareJdbcTemplate", JdbcTemplate.class);
                    jdbcTemplate.execute("SELECT 1 FROM outbox_consumed_events WHERE 1=0");
                });
    }
}