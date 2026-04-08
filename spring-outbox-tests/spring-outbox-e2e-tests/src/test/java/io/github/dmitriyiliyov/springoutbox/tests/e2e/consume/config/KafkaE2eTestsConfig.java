package io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.config;

import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.JdbcConsumerBusinessRepository;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.JpaConsumerBusinessRepositoryProxy;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.config.containers.KafkaTestContainerSingleton;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.kafka.KafkaConsumerBusinessService;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.kafka.KafkaConsumerFaultyBusinessService;
import io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.kafka.KafkaTestUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@TestConfiguration
@Profile("consume-e2e")
public class KafkaE2eTestsConfig {

    private static final String BOOTSTRAP = KafkaTestContainerSingleton.INSTANCE.getBootstrapServers();

    @Bean
    public NewTopic testOutboxSingleTopic() {
        return TopicBuilder.name(KafkaConsumerBusinessService.SINGLE_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic testOutboxBatchTopic() {
        return TopicBuilder.name(KafkaConsumerBusinessService.BATCH_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public KafkaTemplate<String, Object> testKafkaTemplate() {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
        );
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Bean
    public ConsumerFactory<String, Object> testConsumerFactory() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.GROUP_ID_CONFIG, KafkaConsumerBusinessService.CONSUMER_GROUP,
                "spring.json.trusted.packages", "*",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"
        );
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> testSingleKafkaListenerContainerFactory(
            ConsumerFactory<String, Object> testConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setConsumerFactory(testConsumerFactory);
        factory.setBatchListener(false);
         return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> testBatchKafkaListenerContainerFactory(
            ConsumerFactory<String, Object> testConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setConsumerFactory(testConsumerFactory);
        factory.setBatchListener(true);
        return factory;
    }

    @Bean
    public NewTopic testOutboxSingleFailingTopic() {
        return TopicBuilder.name(KafkaConsumerFaultyBusinessService.SINGLE_FAILING_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic testOutboxBatchFailingTopic() {
        return TopicBuilder.name(KafkaConsumerFaultyBusinessService.BATCH_FAILING_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP);
        return new KafkaAdmin(configs);
    }

    @Bean
    public AdminClient adminClient(KafkaAdmin kafkaAdmin) {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    @Bean
    public KafkaTestUtils kafkaTestUtils(AdminClient adminClient) {
        return new KafkaTestUtils(adminClient);
    }

    @Bean
    public KafkaConsumerBusinessService kafkaJdbcConsumerBusinessService(
            OutboxIdempotentConsumer outboxIdempotentConsumer,
            JdbcConsumerBusinessRepository repository
    ) {
        return new KafkaConsumerBusinessService(outboxIdempotentConsumer, repository);
    }

    @Bean
    public KafkaConsumerBusinessService kafkaJpaConsumerBusinessService(
            OutboxIdempotentConsumer outboxIdempotentConsumer,
            JpaConsumerBusinessRepositoryProxy repository
    ) {
        return new KafkaConsumerBusinessService(outboxIdempotentConsumer, repository);
    }

    @Bean
    public KafkaConsumerFaultyBusinessService kafkaJdbcConsumerFaultyBusinessService(
            OutboxIdempotentConsumer outboxIdempotentConsumer,
            JdbcConsumerBusinessRepository repository
    ) {
        return new KafkaConsumerFaultyBusinessService(outboxIdempotentConsumer, repository);
    }

    @Bean
    public KafkaConsumerFaultyBusinessService kafkaJpaConsumerFaultyBusinessService(
            OutboxIdempotentConsumer outboxIdempotentConsumer,
            JpaConsumerBusinessRepositoryProxy repository
    ) {
        return new KafkaConsumerFaultyBusinessService(outboxIdempotentConsumer, repository);
    }
}