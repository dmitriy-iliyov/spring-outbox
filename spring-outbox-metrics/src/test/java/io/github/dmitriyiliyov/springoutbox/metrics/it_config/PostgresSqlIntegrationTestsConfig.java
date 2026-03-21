package io.github.dmitriyiliyov.springoutbox.metrics.it_config;

import io.github.dmitriyiliyov.springoutbox.metrics.publisher.MultiSqlDialectOutboxMetricsRepository;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.OutboxMetricsRepository;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.dlq.MultiSqlDialectOutboxDlqMetricsRepository;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.dlq.OutboxDlqMetricsRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

@TestConfiguration
@Profile("postgres-it")
public class PostgresSqlIntegrationTestsConfig {

    @Bean
    public DataSourceInitializer postgresOutboxDataSourceInitializer(DataSource dataSource) {
        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setEnabled(true);
        dataSourceInitializer.setDataSource(dataSource);
        dataSourceInitializer.setDatabasePopulator(
                new ResourceDatabasePopulator(
                        false,
                        false,
                        StandardCharsets.UTF_8.name(),
                        new Resource[] {
                                new ClassPathResource("psql/psql_outbox_table.sql"),
                                new ClassPathResource("psql/psql_outbox_dlq_table.sql"),
                                new ClassPathResource("psql/psql_outbox_consumed_table.sql")
                        }
                )
        );
        return dataSourceInitializer;
    }

    @Bean
    public JdbcTemplate psqlJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public OutboxDlqMetricsRepository psqlDlqMetricsRepo(@Qualifier("psqlJdbcTemplate") JdbcTemplate jdbcTemplate) {
        return new MultiSqlDialectOutboxDlqMetricsRepository(jdbcTemplate);
    }

    @Bean
    public OutboxMetricsRepository psqlMetricsRepo(@Qualifier("psqlJdbcTemplate") JdbcTemplate jdbcTemplate) {
        return new MultiSqlDialectOutboxMetricsRepository(jdbcTemplate);
    }
}
