package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.core.OutboxScheduler;
import io.github.dmitriyiliyov.springoutbox.core.consumer.*;
import io.github.dmitriyiliyov.springoutbox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.springoutbox.core.locks.OutboxJob;
import io.github.dmitriyiliyov.springoutbox.starter.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
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
@Import({
        OutboxConsumerKafkaAutoConfiguration.class,
        OutboxConsumerRabbitAutoConfiguration.class,
        OutboxConsumerMetricsAutoConfiguration.class,
        OutboxConsumerCacheAutoConfiguration.class,
        OutboxConsumerCacheMetricsAutoConfiguration.class
})
public class OutboxConsumerAutoConfiguration {

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
    @ConditionalOnMissingBean(name = "consumedOutboxManager")
    public ConsumedOutboxManager consumedOutboxManager(ConsumedOutboxRepository repository, Clock clock) {
        return new DefaultConsumedOutboxManager(repository, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxIdempotentConsumer outboxIdempotentConsumer(TransactionTemplate transactionTemplate,
                                                             ConsumedOutboxManager manager) {
        return new DefaultOutboxIdempotentConsumer(transactionTemplate, manager);
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
                        OutboxJobType.CONSUMER_CLEANUP.getValue(),
                        consumerProperties.getCleanUp().getPolling(),
                        executor,
                        scheduleStrategyListenerSupplier
                ),
                manager,
                lockRepository,
                continuableTaskDecoratorSupplier.supply(OutboxJobType.CONSUMER_CLEANUP.getValue())
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