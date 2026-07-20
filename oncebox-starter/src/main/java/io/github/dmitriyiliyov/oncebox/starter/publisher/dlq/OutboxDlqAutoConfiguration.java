package io.github.dmitriyiliyov.oncebox.starter.publisher.dlq;

import io.github.dmitriyiliyov.oncebox.core.OutboxScheduler;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.locks.OutboxJob;
import io.github.dmitriyiliyov.oncebox.core.polling.OutboxScheduleStrategy;
import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxManager;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.*;
import io.github.dmitriyiliyov.oncebox.starter.*;
import io.github.dmitriyiliyov.oncebox.starter.publisher.OutboxPublisherProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ConditionalOnProperty(
        prefix = "oncebox.publisher.dlq",
        name = "enabled",
        havingValue = "true"
)
@Import({
        OutboxDlqMetricsAutoConfiguration.class,
        OutboxDlqApiAutoConfiguration.class,
        OutboxDlqApiMetricsAutoConfiguration.class
})
public class OutboxDlqAutoConfiguration {

    private final OutboxPublisherProperties publisherProperties;

    public OutboxDlqAutoConfiguration(OutboxPublisherProperties publisherProperties) {
        this.publisherProperties = publisherProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqRepository outboxDlqRepository(OutboxRepositoryFactory repositoryFactory) {
        return repositoryFactory.createOutboxDlqRepository();
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxDlqManager")
    public OutboxDlqManager outboxDlqManager(OutboxDlqRepository repository, Clock clock) {
        return new DefaultOutboxDlqManager(repository, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqHandler outboxDlqHandler() {
        return new LogOutboxDlqHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqEventMapper outboxDlqEventMapper(Clock clock) {
        return new DefaultOutboxDlqEventMapper(clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqTransfer outboxDlqTransfer(TransactionTemplate transactionTemplate,
                                               OutboxManager manager,
                                               OutboxDlqManager dlqManager,
                                               OutboxDlqEventMapper mapper,
                                               OutboxDlqHandler handler) {
        return new DefaultOutboxDlqTransfer(transactionTemplate, manager, dlqManager, mapper, handler);
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxDlqTransferToScheduler")
    public OutboxScheduler outboxDlqTransferToScheduler(ScheduledExecutorService executor,
                                                        OutboxDlqTransfer transfer,
                                                        OutboxScheduleStrategyListenerSupplier scheduleStrategyListenerSupplier,
                                                        ContinuableTaskDecoratorSupplier continuableTaskDecoratorSupplier) {
        OutboxPublisherProperties.DlqProperties dlqProperties = publisherProperties.getDlq();
        String taskType = OutboxJobType.TRANSFER_TO_DLQ.getValue();
        OutboxScheduleStrategy scheduleStrategy = OutboxScheduleStrategyFactory.create(
                taskType,
                dlqProperties.getTransferTo().getPolling(),
                executor,
                scheduleStrategyListenerSupplier
        );
        return new OutboxDlqTransferScheduler(
                () -> publisherProperties.getDlq().getTransferTo(),
                scheduleStrategy,
                transfer::transferToDlq,
                continuableTaskDecoratorSupplier.supply(OutboxJobType.TRANSFER_TO_DLQ.getValue()),
                OutboxDlqTransferScheduler.LogMessage.transferTo()
        );
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxDlqTransferFromScheduler")
    public OutboxScheduler outboxDlqTransferFromScheduler(ScheduledExecutorService executor,
                                                          OutboxDlqTransfer transfer,
                                                          OutboxScheduleStrategyListenerSupplier scheduleStrategyListenerSupplier,
                                                          ContinuableTaskDecoratorSupplier continuableTaskDecoratorSupplier) {
        OutboxPublisherProperties.DlqProperties dlqProperties = publisherProperties.getDlq();
        OutboxScheduleStrategy scheduleStrategy = OutboxScheduleStrategyFactory.create(
                OutboxJobType.TRANSFER_FROM_DLQ.getValue(),
                dlqProperties.getTransferFrom().getPolling(),
                executor,
                scheduleStrategyListenerSupplier
        );
        return new OutboxDlqTransferScheduler(
                () -> publisherProperties.getDlq().getTransferFrom(),
                scheduleStrategy,
                transfer::transferFromDlq,
                continuableTaskDecoratorSupplier.supply(OutboxJobType.TRANSFER_FROM_DLQ.getValue()),
                OutboxDlqTransferScheduler.LogMessage.transferFrom()
        );
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "oncebox.publisher.clean-up",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    @ConditionalOnMissingBean(name = "outboxDlqCleanUpScheduler")
    public OutboxScheduler outboxDlqCleanUpScheduler(OutboxProperties properties,
                                                     ScheduledExecutorService executor,
                                                     OutboxDlqManager manager,
                                                     DistributedLockRepository lockRepository,
                                                     OutboxScheduleStrategyListenerSupplier scheduleStrategyListenerSupplier,
                                                     ContinuableTaskDecoratorSupplier continuableTaskDecoratorSupplier) {
        OutboxProperties.CleanUpProperties cleanUpProperties = publisherProperties.getDlq().getCleanUp();
        OutboxScheduleStrategy scheduleStrategy = OutboxScheduleStrategyFactory.create(
                OutboxJobType.DLQ_CLEANUP.getValue(),
                cleanUpProperties.getPolling(),
                executor,
                scheduleStrategyListenerSupplier
        );
        return new OutboxDlqCleanUpScheduler(
                properties.getWorkerId(),
                cleanUpProperties,
                scheduleStrategy,
                continuableTaskDecoratorSupplier.supply(OutboxJobType.DLQ_CLEANUP.getValue()),
                manager,
                lockRepository
        );
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "oncebox.publisher.clean-up",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public OutboxJobCreateCommand outboxDlqCleanUpJobCreateCommand(OutboxProperties properties,
                                                                   @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
                                                                   Clock clock) {
        DistributedLockPropertiesResolver.LockDurations lockDurations = DistributedLockPropertiesResolver.resolve(
                properties.getDistributedLock(),
                publisherProperties.getDlq().getCleanUp().getPolling()
        );
        return new DefaultOutboxJobCreateCommand(
                jdbcTemplate,
                clock,
                OutboxJob.OUTBOX_DLQ_CLEANUP.getJobName(),
                lockDurations.atLeastFor(),
                lockDurations.atMostFor()
        );
    }
}
