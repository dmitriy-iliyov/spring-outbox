package io.github.dmitriyiliyov.springoutbox.core.it_config;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.consumer.PostgreSqlConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.PostgreSqlOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.PostgreSqlOutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.core.utils.DefaultResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.core.utils.PostgreSqlIdHelper;
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
    public OutboxDlqRepository postgresOutboxDlqRepository(DataSource dataSource) {
        return new PostgreSqlOutboxDlqRepository(new JdbcTemplate(dataSource), new PostgreSqlIdHelper(), new DefaultResultSetMapper());
    }

    @Bean
    public OutboxRepository postgresOutboxRepository(DataSource dataSource) {
        return new PostgreSqlOutboxRepository(new JdbcTemplate(dataSource), new PostgreSqlIdHelper(), new DefaultResultSetMapper());
    }

    @Bean
    public ConsumedOutboxRepository postgresConsumedOutboxRepository(DataSource dataSource) {
        return new PostgreSqlConsumedOutboxRepository(new JdbcTemplate(dataSource));
    }
}
