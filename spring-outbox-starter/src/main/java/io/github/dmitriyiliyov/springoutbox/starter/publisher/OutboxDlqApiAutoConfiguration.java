package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import io.github.dmitriyiliyov.springoutbox.core.utils.*;
import io.github.dmitriyiliyov.springoutbox.dlq.api.*;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.dlq.OutboxDlqApiServiceMetricsDecorator;
import io.github.dmitriyiliyov.springoutbox.starter.ConditionalOnDatabaseType;
import io.github.dmitriyiliyov.springoutbox.starter.DatabaseType;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;

@Configuration
@ConditionalOnProperty(
        prefix = "outbox.publisher.dlq",
        name = "enabled",
        havingValue = "true"
)
public class OutboxDlqApiAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OutboxDlqApiAutoConfiguration.class);

    @Bean
    @ConditionalOnClass(OutboxDlqApiRepository.class)
    @ConditionalOnMissingBean
    @ConditionalOnDatabaseType(type = DatabaseType.POSTGRESQL)
    public OutboxDlqApiRepository postgreSqlOutboxDlqApiRepository(@Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
                                                                   Clock clock) {
        return new PostgreSqlOutboxDlqApiRepository(
                jdbcTemplate,
                new PostgreSqlIdHelper(),
                new DefaultResultSetMapper(),
                clock
        );
    }

    @Bean
    @ConditionalOnClass(OutboxDlqApiRepository.class)
    @ConditionalOnMissingBean
    @ConditionalOnDatabaseType(type = DatabaseType.MYSQL)
    public OutboxDlqApiRepository mySqlOutboxDlqApiRepository(@Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
                                                              Clock clock) {
        return new MySqlOutboxDlqApiRepository(
                jdbcTemplate,
                new MySqlIdHelper(),
                new DefaultBytesResultSetMapper(),
                clock
        );
    }

    @Bean
    @ConditionalOnClass(OutboxDlqApiRepository.class)
    @ConditionalOnMissingBean
    @ConditionalOnDatabaseType(type = DatabaseType.ORACLE)
    public OutboxDlqApiRepository oracleOutboxDlqApiRepository(@Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
                                                               Clock clock) {
        return new OracleOutboxDlqApiRepository(
                jdbcTemplate,
                new OracleSqlIdHelper(),
                new DefaultBytesResultSetMapper(),
                clock
        );
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
