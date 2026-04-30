package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.dlq.api.*;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.dlq.OutboxDlqApiServiceMetricsDecorator;
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
    public OutboxDlqApiRepository outboxDlqApiRepository(DataSource dataSource, JdbcTemplate jdbcTemplate, Clock clock) {
        return OutboxDlqApiRepositoryFactory.create(dataSource, jdbcTemplate, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(OutboxDlqApiService.class)
    public OutboxDlqApiService outboxDlqApiService(OutboxDlqApiRepository repository) {
        return new DefaultOutboxDlqApiService(repository);
    }

    @Bean
    @Primary
    @ConditionalOnClass(OutboxDlqApiService.class)
    @ConditionalOnProperty(
            prefix = "outbox.publisher.metrics",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxDlqApiService outboxDlqApiServiceMetricsDecorator(OutboxDlqApiService service,
                                                                   MeterRegistry registry) {
        return new OutboxDlqApiServiceMetricsDecorator(registry, service);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(OutboxDlqController.class)
    public OutboxDlqController outboxDlqController(OutboxDlqApiService service) {
        log.warn("Outbox DLQ API is exposed at '/api/outbox-dlq/events' path should be secured");
        return new OutboxDlqController(service);
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
