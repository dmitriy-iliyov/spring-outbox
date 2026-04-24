package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.dlq.api.*;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.dlq.OutboxDlqApiManagerMetricsDecorator;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.Clock;

@Configuration
@ConditionalOnProperty(prefix = "outbox.publisher.dlq", name = "enabled", havingValue = "true")
public class OutboxDlqApiAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OutboxDlqApiAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(OutboxDlqApiRepository.class)
    public OutboxDlqApiRepository outboxDlqWebRepository(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        return OutboxDlqApiRepositoryFactory.create(dataSource, jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(OutboxDlqApiManager.class)
    public OutboxDlqApiManager outboxDlqWebManager(OutboxDlqApiRepository repository) {
        return new DefaultOutboxDlqApiManager(repository);
    }

    @Bean
    @Primary
    @ConditionalOnClass(OutboxDlqApiManager.class)
    @ConditionalOnProperty(
            prefix = "outbox.publisher.dlq.metrics",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxDlqApiManager outboxDlqWebManagerMetricsDecorator(OutboxDlqApiManager manager,
                                                                   MeterRegistry registry) {
        return new OutboxDlqApiManagerMetricsDecorator(registry, manager);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(OutboxDlqController.class)
    public OutboxDlqController outboxDlqController(OutboxDlqApiManager manager) {
        log.warn("Outbox DLQ API is exposed at '/api/outbox-dlq/events' path should be secured");
        return new OutboxDlqController(manager);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(DlqStatusQueryConverter.class)
    public DlqStatusQueryConverter dlqStatusQueryConverter() {
        return new DlqStatusQueryConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(OutboxDlqControllerAdvice.class)
    public OutboxDlqControllerAdvice outboxDlqControllerAdvice(Clock clock) {
        return new OutboxDlqControllerAdvice(clock);
    }
}
