package io.github.dmitriyiliyov.springoutbox.starter.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.aop.OutboxPublishAspect;
import io.github.dmitriyiliyov.springoutbox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.springoutbox.core.locks.OutboxJob;
import io.github.dmitriyiliyov.springoutbox.core.polling.OutboxScheduleStrategy;
import io.github.dmitriyiliyov.springoutbox.core.publisher.*;
import io.github.dmitriyiliyov.springoutbox.starter.*;
import io.github.dmitriyiliyov.springoutbox.starter.publisher.dlq.OutboxDlqAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ConditionalOnProperty(
        prefix = "outbox.publisher",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Import({
        OutboxPollingSchedulerRegistrar.class,
        OutboxDlqAutoConfiguration.class,
        OutboxPublisherKafkaAutoConfiguration.class,
        OutboxPublisherRabbitAutoConfiguration.class,
        OutboxPublisherMetricsAutoConfiguration.class
})
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
    @ConditionalOnMissingBean(name = "outboxManager")
    public OutboxManager outboxManager(OutboxRepository repository, Clock clock) {
        return new DefaultOutboxManager(repository, clock);
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
    @ConditionalOnMissingBean
    public OutboxProcessor outboxProcessor(OutboxManager manager, OutboxSender sender, Clock clock) {
        return new DefaultOutboxProcessor(manager, sender, clock);
    }

    @Bean
    @Order(2)
    public OutboxPublishAspect outboxPublishAspect(OutboxPublisher publisher) {
        return new OutboxPublishAspect(publisher);
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxRecoveryScheduler")
    public OutboxScheduler outboxRecoveryScheduler(ScheduledExecutorService executor,
                                                   OutboxScheduleStrategyListenerSupplier scheduleStrategyListenerSupplier,
                                                   OutboxManager manager,
                                                   ContinuableTaskDecoratorSupplier continuableTaskDecoratorSupplier) {
        OutboxPublisherProperties.StuckRecoveryProperties stuckRecoveryProperties = publisherProperties.getStuckRecovery();
        OutboxScheduleStrategy strategy = OutboxScheduleStrategyFactory.create(
                OutboxJobType.STUCK_RECOVERY.getValue(),
                stuckRecoveryProperties.getPolling(),
                executor,
                scheduleStrategyListenerSupplier
        );
        ContinuableTaskDecorator continuableTaskDecorator = continuableTaskDecoratorSupplier.supply(OutboxJobType.STUCK_RECOVERY.getValue());
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
                OutboxJobType.PUBLISHER_CLEANUP.getValue(),
                cleanUpProperties.getPolling(),
                executor,
                scheduleStrategyListenerSupplier
        );
        ContinuableTaskDecorator continuableTaskDecorator = continuableTaskDecoratorSupplier.supply(OutboxJobType.PUBLISHER_CLEANUP.getValue());
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
}
