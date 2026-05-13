package io.github.dmitriyiliyov.springoutbox.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.locks.OutboxJob;
import io.github.dmitriyiliyov.springoutbox.starter.consumer.OutboxConsumerAutoConfiguration;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxDlqAutoConfiguration;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.OutboxPublisherAutoConfiguration;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class OutboxJobsInitializerIntegrationVerifier {

    private final String dbUrl;
    private final String dbDriver;
    private final String dbUsername;
    private final String dbPassword;

    public OutboxJobsInitializerIntegrationVerifier(String dbUrl, String dbDriver, String dbUsername, String dbPassword) {
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
                        KafkaAutoConfiguration.class,
                        OutboxAutoConfiguration.class,
                        OutboxPublisherAutoConfiguration.class,
                        OutboxDlqAutoConfiguration.class,
                        OutboxConsumerAutoConfiguration.class
                ))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(Clock.class, Clock::systemDefaultZone)
                .withPropertyValues(
                        "spring.datasource.url=" + dbUrl,
                        "spring.datasource.driver-class-name=" + dbDriver,
                        "spring.datasource.username=" + dbUsername,
                        "spring.datasource.password=" + dbPassword,
                        "outbox.tables.auto-create=true",
                        "outbox.publisher.sender.type=kafka",
                        "outbox.consumer.mappings.test-event=io.github.dmitriyiliyov.springoutbox.starter.consumer.TestEvent",
                        "outbox.publisher.events.my-event.topic=my.topic"
                );
    }

    public void shouldExecuteCommandsAndSaveToDatabase() {
        getBaseContextRunner()
                .run(context -> {
                    assertThat(context).hasSingleBean(ApplicationRunner.class);

                    ApplicationRunner runner = context.getBean("outboxJobsInitializer", ApplicationRunner.class);

                    runner.run(mock(ApplicationArguments.class));

                    JdbcTemplate jdbcTemplate = context.getBean("outboxJdbcTemplate", JdbcTemplate.class);
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM outbox_jobs WHERE job_name = '%s'".formatted(OutboxJob.OUTBOX_PROCESSED_CLEANUP.getJobName()),
                            Integer.class
                    );
                    jdbcTemplate.execute("DROP TABLE outbox_jobs");
                });
    }

    public void shouldExecuteCommandsAndSaveToDatabase_whenDlqEnabled() {
        getBaseContextRunner()
                .withPropertyValues("outbox.publisher.dlq.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ApplicationRunner.class);

                    ApplicationRunner runner = context.getBean("outboxJobsInitializer", ApplicationRunner.class);

                    runner.run(mock(ApplicationArguments.class));

                    JdbcTemplate jdbcTemplate = context.getBean("outboxJdbcTemplate", JdbcTemplate.class);
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM outbox_jobs WHERE job_name = '%s'".formatted(OutboxJob.OUTBOX_DLQ_CLEANUP.getJobName()),
                            Integer.class
                    );
                    assertThat(count).isEqualTo(1);
                });
    }

    public void shouldExecuteCommandsAndSaveToDatabase_whenConsumerEnabled() {
        getBaseContextRunner()
                .withPropertyValues("outbox.consumer.enabled=true", "outbox.consumer.source.type=kafka", "outbox.consumer.clean-up.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ApplicationRunner.class);

                    ApplicationRunner runner = context.getBean("outboxJobsInitializer", ApplicationRunner.class);

                    runner.run(mock(ApplicationArguments.class));

                    JdbcTemplate jdbcTemplate = context.getBean("outboxJdbcTemplate", JdbcTemplate.class);
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM outbox_jobs WHERE job_name = '%s'".formatted(OutboxJob.OUTBOX_CONSUMED_CLEANUP.getJobName()),
                            Integer.class
                    );
                    assertThat(count).isEqualTo(1);
                });
    }
}