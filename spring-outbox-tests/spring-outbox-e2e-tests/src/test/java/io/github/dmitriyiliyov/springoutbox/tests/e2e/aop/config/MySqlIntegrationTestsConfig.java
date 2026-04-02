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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
@Profile("mysql-it")
public class MySqlIntegrationTestsConfig {

    @Bean
    public DataSourceInitializer mysqlOutboxDataSourceInitializer(DataSource dataSource) {
        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setEnabled(true);
        dataSourceInitializer.setDataSource(dataSource);
        dataSourceInitializer.setDatabasePopulator(
                new ResourceDatabasePopulator(
                        false,
                        false,
                        StandardCharsets.UTF_8.name(),
                        new ClassPathResource("mysql/mysql_business_table.sql")
                )
        );
        return dataSourceInitializer;
    }

    @Bean
    public BusinessService mysqlJdbcBusinessService(
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        return new BusinessService(
                new JdbcBusinessRepository(
                        jdbcTemplate,
                        id -> {
                            ByteBuffer bb = ByteBuffer.allocate(16);
                            bb.putLong(id.getMostSignificantBits());
                            bb.putLong(id.getLeastSignificantBits());
                            return bb.array();
                        }
                )
        );
    }

    @Bean
    public BusinessService mysqlJpaBusinessService(@Qualifier("jpaBusinessRepositoryProxy") BusinessRepository repository) {
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
