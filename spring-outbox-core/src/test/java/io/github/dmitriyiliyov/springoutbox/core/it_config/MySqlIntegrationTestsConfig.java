package io.github.dmitriyiliyov.springoutbox.core.it_config;

import io.github.dmitriyiliyov.springoutbox.core.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.consumer.MySqlConsumedOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.MySqlOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.MySqlOutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.core.publisher.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.springoutbox.core.utils.DefaultBytesSqlResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.core.utils.MySqlIdHelper;
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
@Profile("mysql-it")
public class MySqlIntegrationTestsConfig {

    @Bean
    public DataSourceInitializer myslqOutboxDataSourceInitializer(DataSource dataSource) {
        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setEnabled(true);
        dataSourceInitializer.setDataSource(dataSource);
        dataSourceInitializer.setDatabasePopulator(
                new ResourceDatabasePopulator(
                        false,
                        false,
                        StandardCharsets.UTF_8.name(),
                        new Resource[] {
                                new ClassPathResource("mysql/mysql_outbox_table.sql"),
                                new ClassPathResource("mysql/mysql_outbox_dlq_table.sql"),
                                new ClassPathResource("mysql/mysql_outbox_consumed_table.sql")
                        }
                )
        );
        return dataSourceInitializer;
    }

    @Bean
    public OutboxDlqRepository mysqlOutboxDlqRepository(DataSource dataSource) {
        return new MySqlOutboxDlqRepository(new JdbcTemplate(dataSource), new MySqlIdHelper(), new DefaultBytesSqlResultSetMapper());
    }

    @Bean
    public OutboxRepository mysqlOutboxRepository(DataSource dataSource) {
        return new MySqlOutboxRepository(new JdbcTemplate(dataSource), new MySqlIdHelper(), new DefaultBytesSqlResultSetMapper());
    }

    @Bean
    public ConsumedOutboxRepository mysqlConsumedOutboxRepository(DataSource dataSource) {
        return new MySqlConsumedOutboxRepository(new JdbcTemplate(dataSource), new MySqlIdHelper(), new DefaultBytesSqlResultSetMapper());
    }
}
