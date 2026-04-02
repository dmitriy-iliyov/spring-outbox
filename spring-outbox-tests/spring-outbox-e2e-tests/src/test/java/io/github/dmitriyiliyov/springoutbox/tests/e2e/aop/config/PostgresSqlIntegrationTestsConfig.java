package io.github.dmitriyiliyov.springoutbox.tests.e2e.aop.config;

import io.github.dmitriyiliyov.springoutbox.tests.e2e.aop.domain.BusinessRepository;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.aop.domain.BusinessService;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.aop.jdbc.JdbcBusinessRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
                        new ClassPathResource("psql/psql_business_table.sql")
                )
        );
        return dataSourceInitializer;
    }

    @Bean
    public BusinessService postgresJdbcBusinessService(
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        return new BusinessService(
                new JdbcBusinessRepository(jdbcTemplate, id -> id)
        );
    }

    @Bean
    public BusinessService postgresJpaBusinessService(@Qualifier("jpaBusinessRepositoryProxy") BusinessRepository repository) {
        return new BusinessService(repository);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        ProducerFactory<String, Object> producerFactory = mock(ProducerFactory.class);
        when(producerFactory.getConfigurationProperties()).thenReturn(Map.of());
        when(kafkaTemplate.getProducerFactory()).thenReturn(producerFactory);
        return kafkaTemplate;
    }
}
