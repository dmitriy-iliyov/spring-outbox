package io.github.dmitriyiliyov.springoutbox.starter.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.utils.*;
import io.github.dmitriyiliyov.springoutbox.dlq.api.*;
import io.github.dmitriyiliyov.springoutbox.starter.ConditionalOnDatabaseType;
import io.github.dmitriyiliyov.springoutbox.starter.DatabaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;

@Configuration
@ConditionalOnProperty(
        prefix = "outbox.publisher.dlq",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnClass(OutboxDlqController.class)
public class OutboxDlqApiAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OutboxDlqApiAutoConfiguration.class);

    @Bean
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
    @ConditionalOnMissingBean(name = "outboxDlqApiService")
    public OutboxDlqApiService outboxDlqApiService(OutboxDlqApiRepository repository) {
        return new DefaultOutboxDlqApiService(repository);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqController outboxDlqController(OutboxDlqApiService service) {
        log.warn("Outbox DLQ API is exposed at '/api/outbox-dlq/events' path should be secured");
        return new OutboxDlqController(service);
    }

    @Bean
    @ConditionalOnMissingBean
    public DlqStatusQueryConverter dlqStatusQueryConverter() {
        return new DlqStatusQueryConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqControllerAdvice outboxDlqControllerAdvice(Clock clock) {
        return new OutboxDlqControllerAdvice(clock);
    }
}
