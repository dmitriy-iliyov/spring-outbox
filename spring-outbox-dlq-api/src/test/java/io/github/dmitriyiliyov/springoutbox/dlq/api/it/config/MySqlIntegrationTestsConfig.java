package io.github.dmitriyiliyov.springoutbox.dlq.api.it.config;

import io.github.dmitriyiliyov.springoutbox.core.utils.DefaultBytesSqlResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.core.utils.MySqlIdHelper;
import io.github.dmitriyiliyov.springoutbox.dlq.api.MultiDialectOutboxDlqApiRepository;
import io.github.dmitriyiliyov.springoutbox.dlq.api.OutboxDlqApiRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
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
                        new ClassPathResource("mysql/mysql_outbox_dlq_table.sql"))
        );
        return dataSourceInitializer;
    }

    @Bean
    public OutboxDlqApiRepository mysqlOutboxDlqWebRepository(DataSource dataSource) {
        return new MultiDialectOutboxDlqApiRepository(new JdbcTemplate(dataSource), new MySqlIdHelper(), new DefaultBytesSqlResultSetMapper());
    }

    @Bean
    public JdbcTemplate mysqlJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
