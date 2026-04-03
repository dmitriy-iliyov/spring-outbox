package io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.config;

import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxPublisher;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.BusinessRepository;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.JdbcBusinessRepository;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.aop.AopBusinessService;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.manual.ManualBusinessService;
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
    public ManualBusinessService postgresManualJdbcBusinessService(OutboxPublisher publisher, JdbcTemplate jdbcTemplate) {
        return new ManualBusinessService(
                publisher,
                new JdbcBusinessRepository(
                        jdbcTemplate,
                        id -> id
                )
        );
    }

    @Bean
    public ManualBusinessService postgresManualJpaBusinessService(
            OutboxPublisher publisher,
            @Qualifier("jpaBusinessRepositoryProxy") BusinessRepository repository
    ) {
        return new ManualBusinessService(publisher, repository);
    }

    @Bean
    public AopBusinessService postgresAopJdbcBusinessService(
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        return new AopBusinessService(
                new JdbcBusinessRepository(jdbcTemplate, id -> id)
        );
    }

    @Bean
    public AopBusinessService postgresAopJpaBusinessService(@Qualifier("jpaBusinessRepositoryProxy") BusinessRepository repository) {
        return new AopBusinessService(repository);
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
