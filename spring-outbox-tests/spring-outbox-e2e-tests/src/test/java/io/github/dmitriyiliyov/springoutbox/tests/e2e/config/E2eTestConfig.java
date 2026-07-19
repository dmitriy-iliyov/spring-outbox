package io.github.dmitriyiliyov.springoutbox.tests.e2e.config;

import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.springoutbox.core.publisher.OutboxPublisher;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.ConsumerBusinessService;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.domain.E2eEvents;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.publish.PublisherBusinessService;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.repository.TestOutboxRepository;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.repository.TestOutboxRepositoryFactory;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.apache.kafka.clients.admin.NewTopic;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;

@TestConfiguration
public class E2eTestConfig {

    public static final DatabaseContainer CONTAINER = DatabaseContainerFactory.DB_CONTAINER;
    public static final DatabaseType DATABASE_TYPE = CONTAINER.getDatabaseType();

    // The library expects the application to provide a Clock bean
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public DataSourceInitializer e2eBusinessTablesInitializer(DataSource dataSource) {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setEnabled(true);
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(new ResourceDatabasePopulator(
                false,
                false,
                StandardCharsets.UTF_8.name(),
                new ClassPathResource(businessTablesScript())
        ));
        return initializer;
    }

    private String businessTablesScript() {
        return switch (DATABASE_TYPE) {
            case POSTGRES_SQL -> "psql/e2e_business_tables.sql";
            default -> throw new UnsupportedOperationException(
                    "Business tables script for databaseType=" + DATABASE_TYPE + " is not implemented yet"
            );
        };
    }

    @Bean
    public NewTopic e2eEventsTopic() {
        return TopicBuilder.name(E2eEvents.TOPIC).partitions(1).replicas(1).build();
    }

    /**
     * Dedicated sender template with aggressive timeouts so broker outages fail fast
     * and retry/DLQ scenarios do not wait for the default 2-minute delivery timeout.
     */
    @Bean
    public KafkaTemplate<String, String> outboxKafkaTemplate() {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaContainerSingleton.INSTANCE.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
                ProducerConfig.MAX_BLOCK_MS_CONFIG, 2000,
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 2000,
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 4000
        );
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Bean
    public TestOutboxRepository testOutboxRepository(JdbcTemplate jdbcTemplate) {
        return TestOutboxRepositoryFactory.generate(DATABASE_TYPE, jdbcTemplate);
    }

    @Bean
    public PublisherBusinessService publisherBusinessService(OutboxPublisher publisher,
                                                             TestOutboxRepository repository) {
        return new PublisherBusinessService(publisher, repository);
    }

    @Bean
    public ConsumerBusinessService consumerBusinessService(OutboxIdempotentConsumer outboxIdempotentConsumer,
                                                           TestOutboxRepository repository) {
        return new ConsumerBusinessService(outboxIdempotentConsumer, repository);
    }
}
