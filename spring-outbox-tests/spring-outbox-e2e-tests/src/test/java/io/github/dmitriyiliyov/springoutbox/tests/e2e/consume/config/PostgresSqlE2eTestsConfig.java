package io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.config;

import io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.ConsumerBusinessRepository;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.JdbcConsumerBusinessRepository;
import org.springframework.beans.factory.annotation.Qualifier;
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
@Profile("postgres-e2e & consume-e2e")
public class PostgresSqlE2eTestsConfig {

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
                        new ClassPathResource("psql/psql_business_table.sql")
                )
        );
        return dataSourceInitializer;
    }

    @Bean
    public ConsumerBusinessRepository jdbcConsumerBusinessRepository(
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        return new JdbcConsumerBusinessRepository(jdbcTemplate, (id) -> id);
    }
}
