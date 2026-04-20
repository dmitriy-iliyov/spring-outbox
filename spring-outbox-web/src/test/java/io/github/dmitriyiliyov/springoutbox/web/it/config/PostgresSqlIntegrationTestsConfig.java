package io.github.dmitriyiliyov.springoutbox.web.it.config;

import io.github.dmitriyiliyov.springoutbox.core.utils.DefaultResultSetMapper;
import io.github.dmitriyiliyov.springoutbox.core.utils.PostgreSqlIdHelper;
import io.github.dmitriyiliyov.springoutbox.web.MultiDialectOutboxDlqWebRepository;
import io.github.dmitriyiliyov.springoutbox.web.OutboxDlqWebRepository;
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
                        new ClassPathResource("psql/psql_outbox_dlq_table.sql")
                )
        );
        return dataSourceInitializer;
    }

    @Bean
    public JdbcTemplate postgresJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public OutboxDlqWebRepository postgresOutboxDlqWebRepository(DataSource dataSource) {
        return new MultiDialectOutboxDlqWebRepository(new JdbcTemplate(dataSource), new PostgreSqlIdHelper(), new DefaultResultSetMapper());
    }
}
