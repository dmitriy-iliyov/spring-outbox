package io.github.dmitriyiliyov.oncebox.tests.integration.consume.rabbit;

import io.github.dmitriyiliyov.oncebox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.ConsumerBusinessRepository;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.JdbcConsumerBusinessRepository;
import io.github.dmitriyiliyov.oncebox.tests.integration.utils.IdPreparer;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
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
    public Queue testOutboxSingleIdQueue() {
        return QueueBuilder.durable(RabbitConsumerBusinessService.SINGLE_ID_QUEUE).build();
    }

    @Bean
    public Queue testOutboxBatchIdQueue() {
        return QueueBuilder.durable(RabbitConsumerBusinessService.BATCH_ID_QUEUE).build();
    }

    @Bean
    public Queue testOutboxSingleIdFailingQueue() {
        return QueueBuilder.durable(RabbitConsumerFaultyBusinessService.SINGLE_ID_FAILING_QUEUE).build();
    }

    @Bean
    public Queue testOutboxBatchIdFailingQueue() {
        return QueueBuilder.durable(RabbitConsumerFaultyBusinessService.BATCH_ID_FAILING_QUEUE).build();
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
            ConnectionFactory testRabbitConnectionFactory,
            @Qualifier("outboxRabbitMessageConverter") MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(testRabbitConnectionFactory);
        factory.setBatchListener(false);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setMessageConverter(messageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory testBatchRabbitListenerContainerFactory(
            ConnectionFactory testRabbitConnectionFactory,
            @Qualifier("outboxRabbitMessageConverter") MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(testRabbitConnectionFactory);
        factory.setBatchListener(true);
        factory.setConsumerBatchEnabled(true);
        factory.setBatchSize(100);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setMessageConverter(messageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public RabbitConsumerBusinessService rabbitMqJdbcConsumerBusinessService(
            OutboxIdempotentConsumer outboxIdempotentConsumer,
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
            IdPreparer idPreparer
    ) {
        return new RabbitConsumerBusinessService(
                outboxIdempotentConsumer,
                new JdbcConsumerBusinessRepository(jdbcTemplate, idPreparer)
        );
    }

    @Bean
    public RabbitConsumerBusinessService rabbitMqJpaConsumerBusinessService(
            OutboxIdempotentConsumer outboxIdempotentConsumer,
            @Qualifier("jpaConsumerBusinessRepositoryProxy") ConsumerBusinessRepository repository
    ) {
        return new RabbitConsumerBusinessService(
                outboxIdempotentConsumer,
                repository
        );
    }

    @Bean
    public RabbitConsumerFaultyBusinessService rabbitMqJdbcFaultyConsumerBusinessService(
            OutboxIdempotentConsumer outboxIdempotentConsumer,
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
            IdPreparer idPreparer
    ) {
        return new RabbitConsumerFaultyBusinessService(
                outboxIdempotentConsumer,
                new JdbcConsumerBusinessRepository(jdbcTemplate, idPreparer)
        );
    }

    @Bean
    public RabbitConsumerFaultyBusinessService rabbitMqJpaFaultyConsumerBusinessService(
            OutboxIdempotentConsumer outboxIdempotentConsumer,
            @Qualifier("jpaConsumerBusinessRepositoryProxy") ConsumerBusinessRepository repository
    ) {
        return new RabbitConsumerFaultyBusinessService(
                outboxIdempotentConsumer,
                repository
        );
    }
}
