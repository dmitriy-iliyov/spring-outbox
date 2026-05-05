package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.aop.OutboxPublishAspect;
import io.github.dmitriyiliyov.springoutbox.aop.RowOutboxEventListener;
import io.github.dmitriyiliyov.springoutbox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.springoutbox.core.locks.OutboxJob;
import io.github.dmitriyiliyov.springoutbox.core.polling.OutboxScheduleStrategy;
import io.github.dmitriyiliyov.springoutbox.core.publisher.*;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.springoutbox.core.publisher.utils.UuidGenerator;
import io.github.dmitriyiliyov.springoutbox.core.publisher.utils.UuidV7Generator;
import io.github.dmitriyiliyov.springoutbox.kafka.KafkaOutboxSender;
import io.github.dmitriyiliyov.springoutbox.metrics.MetricsTaskType;
import io.github.dmitriyiliyov.springoutbox.metrics.OutboxMetrics;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.*;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.NoopOutboxCache;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.OutboxCache;
import io.github.dmitriyiliyov.springoutbox.metrics.publisher.utils.SimpleOutboxCache;
import io.github.dmitriyiliyov.springoutbox.rabbit.RabbitOutboxSender;
import io.github.dmitriyiliyov.springoutbox.starter.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ConditionalOnProperty(
        prefix = "outbox.publisher",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxPublisherAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherAutoConfiguration.class);

    private final OutboxPublisherProperties publisherProperties;
    private final ObjectMapper mapper;

    public OutboxPublisherAutoConfiguration(OutboxPublisherProperties publisherProperties, ObjectMapper mapper) {
        this.publisherProperties = publisherProperties;
        this.mapper = mapper;
        log.debug("OutboxPublisherAutoConfiguration successfully created");
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxRepository outboxRepository(OutboxRepositoryFactory repositoryFactory) {
        return repositoryFactory.createOutboxRepository();
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxCache")
    public OutboxCache<EventStatus> outboxCache() {
        OutboxProperties.MetricsProperties metricsProperties = publisherProperties.getMetrics();
        if (metricsProperties != null) {
            OutboxProperties.MetricsProperties.GaugeProperties gaugeProperties = metricsProperties.getGauge();
            if (gaugeProperties != null) {
                if (gaugeProperties.isEnabled()) {
                    List<Duration> ttls = gaugeProperties.getCache().getTtls();
                    if (ttls == null || ttls.isEmpty()) {
                        throw new IllegalArgumentException("Cache ttls cannot be null or empty");
                    }
                    if (ttls.size() != 3) {
                        throw new IllegalArgumentException("Ttls should be 3 element size");
                    }
                    return new SimpleOutboxCache<>(
                            ttls.get(0).toSeconds(), ttls.get(1).toSeconds(), ttls.get(2).toSeconds()
                    );
                }
            }
        }
        return new NoopOutboxCache<>();
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxManager outboxManager(OutboxRepository repository, Clock clock) {
        return new DefaultOutboxManager(repository, clock);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(
            prefix = "outbox.publisher.metrics",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxManager outboxManagerMetricsDecorator(OutboxManager manager, MeterRegistry registry) {
        return new OutboxManagerMetricsDecorator(publisherProperties, registry, manager);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "outbox.publisher.sender", name = "type", havingValue = "kafka")
    public OutboxSender kafkaOutboxSender(ApplicationContext context, ObjectMapper mapper) {
        OutboxPublisherProperties.SenderProperties senderProperties = publisherProperties.getSender();
        String beanName = senderProperties.getBeanName();
        KafkaTemplate<String, Object> kafkaTemplate;
        if (beanName == null || beanName.isEmpty()) {
            log.warn("Sender bean-name is not specified. Will try to resolve by type");
            String [] beanNames = context.getBeanNamesForType(KafkaTemplate.class);
            if (beanNames.length == 0) {
                throw new IllegalStateException("Cannot create OutboxSender: no KafkaTemplate bean found");
            }
            if (beanNames.length > 1) {
                throw new IllegalStateException(
                        "Cannot create OutboxSender: found more then one KafkaTemplate bean: " +
                                Arrays.toString(beanNames) +
                                "Please define a KafkaTemplate<String, Object> bean with this name, " +
                                "or configure 'outbox.sender.bean-name' property"
                );
            }
            beanName = beanNames[0];
        }
        if (!context.containsBean(beanName)) {
            throw new IllegalArgumentException(
                    "Cannot create OutboxSender: KafkaTemplate bean '" + beanName + "' not found. " +
                            "Please define a KafkaTemplate<String, Object> bean with this name, " +
                            "or configure 'outbox.sender.bean-name' property"
            );
        }
        senderProperties.setBeanName(beanName);
        kafkaTemplate = context.getBean(beanName, KafkaTemplate.class);
        Map<String, Object> configs = kafkaTemplate.getProducerFactory().getConfigurationProperties();
        String acks = (String) configs.get("acks");
        if (acks == null || !acks.equals("all")) {
            log.warn("Kafka producer factory is configured without 'acks=all'. Outbox cannot guarantee at-least-once delivery");
        }
        Boolean idempotence = null;
        Object idempotenceObj = configs.get("enable.idempotence");
        if (idempotenceObj instanceof Boolean) {
            idempotence = (Boolean) idempotenceObj;
        } else if (idempotenceObj instanceof String) {
            idempotence = Boolean.parseBoolean((String) idempotenceObj);
        }
        if (idempotence == null || !idempotence) {
            log.warn("Kafka producer is not idempotent. It is recommended to enabled 'enabled.idempotence=true' to avoid message duplication");
        }
        return new KafkaOutboxSender(kafkaTemplate, senderProperties.getEmergencyTimeout().toSeconds(), mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "outbox.publisher.sender", name = "type", havingValue = "rabbit")
    public OutboxSender rabbitOutboxSender(ApplicationContext context) {
        OutboxPublisherProperties.SenderProperties senderProperties = publisherProperties.getSender();
        String beanName = senderProperties.getBeanName();
        RabbitTemplate rabbitTemplate;
        if (beanName == null || beanName.isEmpty()) {
            log.warn("Sender bean-name is not specified. Will try to resolve by type");
            String [] beanNames = context.getBeanNamesForType(RabbitTemplate.class);
            if (beanNames.length == 0) {
                throw new IllegalStateException("Cannot create OutboxSender: no RabbitTemplate bean found");
            }
            if (beanNames.length > 1) {
                throw new IllegalStateException(
                        "Cannot create OutboxSender: found more then one RabbitTemplate bean: " +
                                Arrays.toString(beanNames) +
                                "Please define a RabbitTemplate bean with this name, " +
                                "or configure 'outbox.sender.bean-name' property"
                );
            }
            beanName = beanNames[0];
        }
        if (!context.containsBean(beanName)) {
            throw new IllegalArgumentException(
                    "Cannot create OutboxSender: RabbitTemplate bean '" + beanName + "' not found. " +
                            "Please define a RabbitTemplate bean with this name, " +
                            "or configure 'outbox.sender.bean-name' property"
            );
        }
        senderProperties.setBeanName(beanName);
        rabbitTemplate = context.getBean(beanName, RabbitTemplate.class);
        if (!rabbitTemplate.isMandatoryFor(new Message(Boolean.FALSE.toString().getBytes(StandardCharsets.UTF_8)))) {
            log.error("RabbitTemplate '{}' mandatory flag is false. " +
                    "ReturnedMessage will not be received. You should set mandatory=true for at-least-once", beanName);
        }
        return new RabbitOutboxSender(rabbitTemplate, senderProperties.getEmergencyTimeout().toSeconds());
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxProcessor outboxProcessor(OutboxManager manager, OutboxSender sender, Clock clock) {
        return new DefaultOutboxProcessor(manager, sender, clock);
    }

    @Bean
    public SmartInitializingSingleton outboxSchedulersInitializer(
            @Qualifier("outboxScheduledExecutorService") ScheduledExecutorService executor,
            OutboxProcessor processor,
            ConfigurableListableBeanFactory factory,
            OutboxScheduleStrategyListenerSupplier scheduleStrategyListenerSupplier,
            ContinuableTaskDecoratorSupplier continuableTaskDecoratorSupplier
    ) {
        return () -> {
            log.debug("Start initialize schedulers beans");

            for (OutboxPublisherProperties.EventProperties event : publisherProperties.getEvents().values()) {
                String beanName = BeanNameUtils.toBeanName(event.getEventType(), "OutboxPublisherScheduler");
                if (!factory.containsBean(beanName)) {
                    registerOutboxPollingScheduler(
                            factory, beanName, event, executor, scheduleStrategyListenerSupplier, processor,
                            continuableTaskDecoratorSupplier
                    );
                    log.debug("Created bean with beanName {}", beanName);
                }
            }

            log.debug("Schedulers beans successfully initialized");
        };
    }
    
    private void registerOutboxPollingScheduler(ConfigurableListableBeanFactory factory,
                                                String beanName,
                                                OutboxPublisherProperties.EventProperties event,
                                                ScheduledExecutorService executor,
                                                OutboxScheduleStrategyListenerSupplier scheduleStrategyListenerSupplier,
                                                OutboxProcessor processor,
                                                ContinuableTaskDecoratorSupplier continuableTaskDecoratorSupplier) {
        OutboxScheduleStrategy strategy = OutboxScheduleStrategyFactory.create(
                event.getEventType(),
                event.getPolling(),
                executor,
                scheduleStrategyListenerSupplier
        );
        ContinuableTaskDecorator continuableTaskDecorator = continuableTaskDecoratorSupplier.supply(event.getEventType());
        factory.registerSingleton(
                beanName,
                new OutboxPollingScheduler(event, strategy, processor, continuableTaskDecorator)
        );
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxRecoveryScheduler")
    public OutboxScheduler outboxRecoveryScheduler(ScheduledExecutorService executor,
                                                   OutboxScheduleStrategyListenerSupplier scheduleStrategyListenerSupplier,
                                                   OutboxManager manager,
                                                   ContinuableTaskDecoratorSupplier continuableTaskDecoratorSupplier) {
        OutboxPublisherProperties.StuckRecoveryProperties stuckRecoveryProperties = publisherProperties.getStuckRecovery();
        OutboxScheduleStrategy strategy = OutboxScheduleStrategyFactory.create(
                MetricsTaskType.STUCK_RECOVERY.getValue(),
                stuckRecoveryProperties.getPolling(),
                executor,
                scheduleStrategyListenerSupplier
        );
        ContinuableTaskDecorator continuableTaskDecorator = continuableTaskDecoratorSupplier.supply(MetricsTaskType.STUCK_RECOVERY.getValue());
        return new OutboxRecoveryScheduler(stuckRecoveryProperties, strategy, manager, continuableTaskDecorator);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "outbox.publisher.clean-up",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    @ConditionalOnMissingBean(name = "outboxCleanUpScheduler")
    public OutboxScheduler outboxCleanUpScheduler(OutboxProperties properties,
                                                  ScheduledExecutorService executor,
                                                  OutboxScheduleStrategyListenerSupplier scheduleStrategyListenerSupplier,
                                                  OutboxManager manager,
                                                  DistributedLockRepository lockRepository,
                                                  ContinuableTaskDecoratorSupplier continuableTaskDecoratorSupplier) {
        OutboxProperties.CleanUpProperties cleanUpProperties = publisherProperties.getCleanUp();
        OutboxScheduleStrategy strategy = OutboxScheduleStrategyFactory.create(
                MetricsTaskType.PUBLISHER_CLEANUP.getValue(),
                cleanUpProperties.getPolling(),
                executor,
                scheduleStrategyListenerSupplier
        );
        ContinuableTaskDecorator continuableTaskDecorator = continuableTaskDecoratorSupplier.supply(MetricsTaskType.PUBLISHER_CLEANUP.getValue());
        return new OutboxCleanUpScheduler(
                properties.getWorkerId(), cleanUpProperties, strategy, manager, lockRepository, continuableTaskDecorator
        );
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "outbox.publisher.clean-up",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public OutboxJobCreateCommand outboxCleanUpJobCreateCommand(OutboxProperties properties,
                                                                @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
                                                                Clock clock) {
        DistributedLockPropertiesResolver.LockDurations lockDurations = DistributedLockPropertiesResolver.resolve(
                properties.getDistributedLock(),
                publisherProperties.getCleanUp().getPolling()
        );
        return new DefaultOutboxJobCreateCommand(
                jdbcTemplate,
                clock,
                OutboxJob.OUTBOX_PROCESSED_CLEANUP.getJobName(),
                lockDurations.atLeastFor(),
                lockDurations.atMostFor()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public UuidGenerator outboxUuidGenerator() {
        return new UuidV7Generator();
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxSerializer outboxSerializer(UuidGenerator uuidGenerator, Clock clock) {
        return new JacksonOutboxSerializer(mapper, uuidGenerator, clock);
    }

    @Bean
    public OutboxPublisher outboxPublisher(OutboxSerializer serializer, OutboxManager manager) {
        return new DefaultOutboxPublisher(publisherProperties, serializer, manager);
    }

    @Bean
    public OutboxPublishAspect outboxEventAspect(ApplicationEventPublisher eventPublisher) {
        return new OutboxPublishAspect(eventPublisher);
    }

    @Bean
    public RowOutboxEventListener rowOutboxEventListener(OutboxPublisher publisher) {
        return new RowOutboxEventListener(publisher);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "outbox.publisher.metrics",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnProperty(
            prefix = "outbox.publisher.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxMetricsRepository outboxMetricsRepository(
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        return new MultiDialectOutboxMetricsRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "outbox.publisher.metrics",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnProperty(
            prefix = "outbox.publisher.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxMetricsService outboxMetricsService(OutboxMetricsRepository repository, OutboxCache<EventStatus> cache) {
        return new DefaultOutboxMetricsService(repository, cache);
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxMetrics")
    @ConditionalOnProperty(
            prefix = "outbox.publisher.metrics",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnProperty(
            prefix = "outbox.publisher.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxMetrics outboxMetrics(MeterRegistry registry, OutboxMetricsService metricsService) {
        return new DefaultOutboxMetrics(publisherProperties, registry, metricsService);
    }
}
