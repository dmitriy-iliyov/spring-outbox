package io.github.dmitriyiliyov.springoutbox.tests.integration.consume.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.springoutbox.tests.integration.consume.shared.ConsumerBusinessRepository;
import io.github.dmitriyiliyov.springoutbox.tests.integration.consume.shared.JdbcConsumerBusinessRepository;
import io.github.dmitriyiliyov.springoutbox.tests.integration.domain.BusinessEvent;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.RabbitMQContainer;

@TestConfiguration
public class RabbitIntegrationTestsConfig {

    private static final RabbitMQContainer RABBIT = RabbitTestContainerSingleton.INSTANCE;

    @Bean
    public ConnectionFactory testRabbitConnectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(RABBIT.getHost());
        factory.setPort(RABBIT.getAmqpPort());
        factory.setUsername(RABBIT.getAdminUsername());
        factory.setPassword(RABBIT.getAdminPassword());
        return factory;
    }

    @Bean
    public RabbitAdmin testRabbitAdmin(ConnectionFactory testRabbitConnectionFactory) {
        return new RabbitAdmin(testRabbitConnectionFactory);
    }

    @Bean
    public RabbitTemplate testRabbitTemplate(ConnectionFactory testRabbitConnectionFactory) {
        return new RabbitTemplate(testRabbitConnectionFactory);
    }

    @Bean
    public Queue testOutboxSingleQueue() {
        return QueueBuilder.durable(RabbitConsumerBusinessService.SINGLE_QUEUE).build();
    }

    @Bean
    public Queue testOutboxBatchQueue() {
        return QueueBuilder.durable(RabbitConsumerBusinessService.BATCH_QUEUE).build();
    }

    @Bean
    public Queue testOutboxSingleFailingQueue() {
        return QueueBuilder.durable(RabbitConsumerFaultyBusinessService.SINGLE_FAILING_QUEUE).build();
    }

    @Bean
    public Queue testOutboxBatchFailingQueue() {
        return QueueBuilder.durable(RabbitConsumerFaultyBusinessService.BATCH_FAILING_QUEUE).build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory testSingleRabbitListenerContainerFactory(
            ConnectionFactory testRabbitConnectionFactory
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(testRabbitConnectionFactory);
        factory.setBatchListener(false);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory testBatchRabbitListenerContainerFactory(
            ConnectionFactory testRabbitConnectionFactory
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(testRabbitConnectionFactory);
        factory.setBatchListener(true);
        factory.setConsumerBatchEnabled(true);
        factory.setBatchSize(10);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        return factory;
    }

    @Bean
    public RabbitConsumerBusinessService.MessageConverter testRabbitMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        return message -> {
            try {
                return mapper.readValue(message.getBody(), BusinessEvent.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize BusinessEvent", e);
            }
        };
    }

    @Bean
    public RabbitConsumerBusinessService rabbitMqJdbcConsumerBusinessService(
            OutboxIdempotentConsumer outboxIdempotentConsumer,
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
            RabbitConsumerBusinessService.MessageConverter testRabbitMessageConverter
    ) {
        return new RabbitConsumerBusinessService(
                outboxIdempotentConsumer,
                new JdbcConsumerBusinessRepository(jdbcTemplate, id -> id),
                testRabbitMessageConverter
        );
    }

    @Bean
    public RabbitConsumerBusinessService rabbitMqJpaConsumerBusinessService(
            OutboxIdempotentConsumer outboxIdempotentConsumer,
            @Qualifier("jpaConsumerBusinessRepositoryProxy") ConsumerBusinessRepository repository,
            RabbitConsumerBusinessService.MessageConverter testRabbitMessageConverter
    ) {
        return new RabbitConsumerBusinessService(
                outboxIdempotentConsumer,
                repository,
                testRabbitMessageConverter
        );
    }

    @Bean
    public RabbitConsumerFaultyBusinessService rabbitMqJdbcFaultyConsumerBusinessService(
            OutboxIdempotentConsumer outboxIdempotentConsumer,
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
            RabbitConsumerBusinessService.MessageConverter testRabbitMessageConverter
    ) {
        return new RabbitConsumerFaultyBusinessService(
                outboxIdempotentConsumer,
                new JdbcConsumerBusinessRepository(jdbcTemplate, id -> id),
                testRabbitMessageConverter
        );
    }

    @Bean
    public RabbitConsumerFaultyBusinessService rabbitMqJpaFaultyConsumerBusinessService(
            OutboxIdempotentConsumer outboxIdempotentConsumer,
            @Qualifier("jpaConsumerBusinessRepositoryProxy") ConsumerBusinessRepository repository,
            RabbitConsumerBusinessService.MessageConverter testRabbitMessageConverter
    ) {
        return new RabbitConsumerFaultyBusinessService(
                outboxIdempotentConsumer,
                repository,
                testRabbitMessageConverter
        );
    }
}
