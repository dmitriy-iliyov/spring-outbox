package io.github.dmitriyiliyov.springoutbox.starter;


import io.github.dmitriyiliyov.springoutbox.starter.consumer.OutboxConsumerProperties;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxPublisherProperties;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

public class OutboxAutoConfigurationVerifier {

    private final String dbUrl;
    private final String dbDriver;
    private final String dbUsername;
    private final String dbPassword;

    public OutboxAutoConfigurationVerifier(String dbUrl, String dbDriver, String dbUsername, String dbPassword) {
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
                        OutboxAutoConfiguration.class
                ))
                .withPropertyValues(
                        "spring.datasource.url=" + dbUrl,
                        "spring.datasource.driver-class-name=" + dbDriver,
                        "spring.datasource.username=" + dbUsername,
                        "spring.datasource.password=" + dbPassword,
                        "outbox.tables.auto-create=false"
                );
    }

    public void shouldRegisterOutboxPublisherPropertiesBean() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxPublisherProperties.class)
        );
    }

    public void shouldRegisterOutboxConsumerPropertiesBean() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxConsumerProperties.class)
        );
    }

    public void shouldRegisterJdbcTemplate() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasBean("outboxJdbcTemplate")
        );
    }

    public void shouldRegisterScheduledExecutorServiceBean() {
        getBaseContextRunner()
                .withPropertyValues("outbox.thread-pool-size=3")
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(ScheduledExecutorService.class)
                );
    }

    public void shouldRegisterOutboxInitializerWhenMissing() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(PostApplicationStartOutboxInitializer.class)
        );
    }

    public void shouldNotRegisterOutboxInitializerWhenAlreadyPresent() {
        getBaseContextRunner()
                .withBean(PostApplicationStartOutboxInitializer.class,
                        () -> new PostApplicationStartOutboxInitializer(null))
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(PostApplicationStartOutboxInitializer.class)
                );
    }

    public void shouldNotRegisterDataSourceInitializerWhenAutoCreateFalse() {
        getBaseContextRunner()
                .withPropertyValues("outbox.tables.auto-create=false")
                .run(ctx ->
                        assertThat(ctx).doesNotHaveBean("outboxDataSourceInitializer")
                );
    }

    public void shouldCreateTablesWhenAutoCreateTrue() {
        getBaseContextRunner()
                .withPropertyValues("outbox.tables.auto-create=true")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    JdbcTemplate jdbcTemplate = ctx.getBean("outboxJdbcTemplate", JdbcTemplate.class);
                    jdbcTemplate.execute("SELECT 1 FROM outbox_events WHERE 1=0");
                });
    }

    public void jdbcTemplateShouldBeTransactionAware() {
        getBaseContextRunner().run(ctx -> {
            JdbcTemplate jdbcTemplate = ctx.getBean("outboxJdbcTemplate", JdbcTemplate.class);
            assertThat(jdbcTemplate.getDataSource()).isInstanceOf(TransactionAwareDataSourceProxy.class);
        });
    }

    public void publisherBeanShouldBeDisabledByDefault() {
        getBaseContextRunner().run(ctx -> {
            OutboxPublisherProperties publisher = ctx.getBean(OutboxPublisherProperties.class);
            assertThat(publisher.isEnabled()).isFalse();
        });
    }

    public void consumerBeanShouldBeDisabledByDefault() {
        getBaseContextRunner().run(ctx -> {
            OutboxConsumerProperties consumer = ctx.getBean(OutboxConsumerProperties.class);
            assertThat(consumer.isEnabled()).isFalse();
        });
    }

    public void shouldFailWhenDataSourceNotAvailable() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class))
                .run(ctx ->
                        assertThat(ctx).hasFailed()
                );
    }

    public void shouldUseDefaultThreadPoolSizeWhenNotConfigured() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(ScheduledExecutorService.class)
        );
    }
}
