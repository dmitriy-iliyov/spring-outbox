package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.consumer.cache.ConsumedOutboxCacheListener;
import io.github.dmitriyiliyov.springoutbox.consumer.cache.DefaultConsumedOutboxCache;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.core.consumer.*;
import io.github.dmitriyiliyov.springoutbox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.springoutbox.core.locks.OutboxJob;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxHeaders;
import io.github.dmitriyiliyov.springoutbox.metrics.MetricsTaskType;
import io.github.dmitriyiliyov.springoutbox.metrics.consumer.ConsumedOutboxManagerMetricsDecorator;
import io.github.dmitriyiliyov.springoutbox.metrics.consumer.MetricsConsumedOutboxCacheListener;
import io.github.dmitriyiliyov.springoutbox.starter.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper;
import org.springframework.kafka.support.mapping.Jackson2JavaTypeMapper;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ConditionalOnProperty(
        prefix = "outbox.consumer",
        name = "enabled",
        havingValue = "true"
)
public class OutboxConsumerAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OutboxConsumerAutoConfiguration.class);

    private final OutboxConsumerProperties consumerProperties;

    public OutboxConsumerAutoConfiguration(OutboxConsumerProperties consumerProperties) {
        this.consumerProperties = consumerProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsumedOutboxRepository consumedOutboxRepository(OutboxRepositoryFactory repositoryFactory) {
        return repositoryFactory.createConsumedOutboxRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsumedOutboxManager consumedOutboxManager(ConsumedOutboxRepository repository, Clock clock) {
        return new DefaultConsumedOutboxManager(repository, clock);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(
            prefix = "outbox.consumer.metrics",
            name = "enabled",
            havingValue = "true"
    )
    public ConsumedOutboxManager consumedOutboxManagerMetricsDecorator(MeterRegistry registry,
                                                                       ConsumedOutboxManager consumedOutboxManager) {
        return new ConsumedOutboxManagerMetricsDecorator(registry, consumedOutboxManager);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "outbox.consumer.metrics",
            name = "enabled",
            havingValue = "true"
    )
    public ConsumedOutboxCacheListener metricsConsumedOutboxCacheListener(MeterRegistry registry) {
        return new MetricsConsumedOutboxCacheListener(registry);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "outbox.consumer.metrics",
            name = "enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    public ConsumedOutboxCacheListener noopConsumedOutboxCacheListener() {
        return ConsumedOutboxCacheListener.NOOP;
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxIdempotentConsumer outboxIdempotentConsumer(TransactionTemplate transactionTemplate,
                                                             ConsumedOutboxManager manager) {
        return new DefaultOutboxIdempotentConsumer(transactionTemplate, manager);
    }

    @Bean
    @Order(1)
    @ConditionalOnProperty(
            prefix = "outbox.consumer.cache",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxIdempotentConsumerDecoratorSupplier outboxIdempotentConsumerCacheDecoratorSupplier(
            ConsumedOutboxCacheListener cacheListener,
            CacheManager cacheManager
    ) {
        return new OutboxIdempotentConsumerCacheDecoratorSupplier(
                new DefaultConsumedOutboxCache(
                        cacheManager,
                        consumerProperties.getCache().getCacheName(),
                        cacheListener
                )
        );
    }

    @Bean
    @Order(2)
    @ConditionalOnProperty(
            prefix = "outbox.consumer.metrics",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxIdempotentConsumerDecoratorSupplier outboxIdempotentConsumerMetricsDecoratorSupplier(MeterRegistry registry) {
        return new OutboxIdempotentConsumerMetricsDecoratorSupplier(registry);
    }

    @Bean
    @Primary
    public OutboxIdempotentConsumer primaryOutboxIdempotentConsumer(
            OutboxIdempotentConsumer consumer,
            List<OutboxIdempotentConsumerDecoratorSupplier> decoratorSuppliers
    ) {
        OutboxIdempotentConsumer primaryConsumer = consumer;
        for (OutboxIdempotentConsumerDecoratorSupplier supplier : decoratorSuppliers) {
            primaryConsumer = supplier.supply(primaryConsumer);
        }
        return primaryConsumer;
    }

    @Bean(name = "outboxKafkaRecordMessageConverter")
    @ConditionalOnMissingBean(name = "outboxKafkaRecordMessageConverter")
    @ConditionalOnProperty(prefix = "outbox.consumer.source", name = "type", havingValue = "kafka")
    public RecordMessageConverter outboxKafkaRecordMessageConverter(ObjectMapper objectMapper) {
        StringJsonMessageConverter converter = new StringJsonMessageConverter(objectMapper);

        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.TYPE_ID);
        typeMapper.setClassIdFieldName(OutboxHeaders.EVENT_TYPE.getValue());
        typeMapper.setIdClassMapping(consumerProperties.getMappings());

        converter.setTypeMapper(typeMapper);
        return converter;
    }

    @Bean(name = "outboxKafkaListenerContainerFactory")
    @ConditionalOnMissingBean(name = "outboxKafkaListenerContainerFactory")
    @ConditionalOnProperty(prefix = "outbox.consumer.source", name = "type", havingValue = "kafka")
    public ConcurrentKafkaListenerContainerFactory<Object, Object> outboxKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory,
            @Qualifier("outboxKafkaRecordMessageConverter") RecordMessageConverter recordMessageConverter
    ) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);
        factory.setRecordMessageConverter(recordMessageConverter);
        if (Boolean.TRUE.equals(factory.isBatchListener())) {
            factory.setBatchMessageConverter(new BatchMessagingMessageConverter(recordMessageConverter));
        } else {
            factory.setRecordMessageConverter(recordMessageConverter);
        }
        if (!ContainerProperties.AckMode.MANUAL.equals(factory.getContainerProperties().getAckMode()) &&
                !ContainerProperties.AckMode.MANUAL_IMMEDIATE.equals(factory.getContainerProperties().getAckMode())) {
            log.warn("Outbox Consumer AckMode isn't MANUAL. It is highly recommended to set " +
                    "'spring.kafka.listener.ack-mode=manual' for 'at-least-once' delivery guarantees.");
        }
        return factory;
    }

    @Bean(name = "outboxRabbitMessageConverter")
    @ConditionalOnMissingBean(name = "outboxRabbitMessageConverter")
    @ConditionalOnProperty(prefix = "outbox.consumer.source", name = "type", havingValue = "rabbit")
    public MessageConverter outboxRabbitMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setClassMapper(new OutboxRabbitClassMapper(consumerProperties.getMappings()));
        return converter;
    }

    @Bean(name = "outboxRabbitListenerContainerFactory")
    @ConditionalOnMissingBean(name = "outboxRabbitListenerContainerFactory")
    @ConditionalOnProperty(prefix = "outbox.consumer.source", name = "type", havingValue = "rabbit")
    public RabbitListenerContainerFactory<?> outboxRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            @Qualifier("outboxRabbitMessageConverter") MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "outbox.consumer.clean-up",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxScheduler consumedOutboxCleanUpScheduler(
            OutboxProperties properties,
            @Qualifier("outboxScheduledExecutorService") ScheduledExecutorService executor,
            ConsumedOutboxManager manager,
            OutboxScheduleStrategyListenerSupplier scheduleStrategyListenerSupplier,
            ContinuableTaskDecoratorSupplier continuableTaskDecoratorSupplier,
            DistributedLockRepository lockRepository
    ) {
        return new ConsumedOutboxCleanUpScheduler(
                properties.getWorkerId(),
                consumerProperties.getCleanUp(),
                OutboxScheduleStrategyFactory.create(
                        MetricsTaskType.CONSUMER_CLEANUP.getValue(),
                        consumerProperties.getCleanUp().getPolling(),
                        executor,
                        scheduleStrategyListenerSupplier
                ),
                manager,
                lockRepository,
                continuableTaskDecoratorSupplier.supply(MetricsTaskType.CONSUMER_CLEANUP.getValue())
        );
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "outbox.consumer.clean-up",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxJobCreateCommand consumedOutboxCleanUpJobCreateCommand(OutboxProperties properties,
                                                                        @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
                                                                        Clock clock) {
        DistributedLockPropertiesResolver.LockDurations lockDurations = DistributedLockPropertiesResolver.resolve(
                properties.getDistributedLock(),
                consumerProperties.getCleanUp().getPolling()
        );
        return new DefaultOutboxJobCreateCommand(
                jdbcTemplate,
                clock,
                OutboxJob.OUTBOX_CONSUMED_CLEANUP.getJobName(),
                lockDurations.atLeastFor(),
                lockDurations.atMostFor()
        );
    }
}