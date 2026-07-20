package io.github.dmitriyiliyov.oncebox.starter.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.oncebox.core.OutboxScheduler;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.polling.OutboxScheduleStrategy;
import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxCleanUpScheduler;
import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxManager;
import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxRecoveryScheduler;
import io.github.dmitriyiliyov.oncebox.starter.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherAutoConfigurationUnitTests {

    private OutboxPublisherProperties props;
    private OutboxPublisherAutoConfiguration config;

    @Mock
    private ScheduledExecutorService executor;

    @Mock
    private OutboxScheduleStrategyListenerSupplier scheduleStrategyListenerSupplier;

    @Mock
    private ContinuableTaskDecoratorSupplier continuableTaskDecoratorSupplier;

    @Mock
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        props = mock(OutboxPublisherProperties.class);
        config = new OutboxPublisherAutoConfiguration(props, mapper);
    }

    @Test
    @DisplayName("UT outboxRecoveryScheduler creates OutboxRecoveryScheduler")
    void outboxRecoveryScheduler_createsScheduler(@Mock OutboxManager manager) {
        OutboxPublisherProperties.StuckRecoveryProperties stuckRecoveryProperties = mock(OutboxPublisherProperties.StuckRecoveryProperties.class);
        when(stuckRecoveryProperties.getPolling()).thenReturn(mock(OutboxProperties.PollingProperties.class));
        when(props.getStuckRecovery()).thenReturn(stuckRecoveryProperties);
        when(continuableTaskDecoratorSupplier.supply(anyString())).thenReturn(mock(ContinuableTaskDecorator.class));

        try (MockedStatic<OutboxScheduleStrategyFactory> strategyFactory = mockStatic(OutboxScheduleStrategyFactory.class)) {
            strategyFactory.when(() -> OutboxScheduleStrategyFactory.create(any(), any(), any(), any()))
                    .thenReturn(mock(OutboxScheduleStrategy.class));

            OutboxScheduler scheduler = config.outboxRecoveryScheduler(
                    executor, scheduleStrategyListenerSupplier, manager, continuableTaskDecoratorSupplier
            );

            assertThat(scheduler).isInstanceOf(OutboxRecoveryScheduler.class);
        }
    }

    @Test
    @DisplayName("UT outboxCleanUpScheduler creates OutboxCleanUpScheduler")
    void outboxCleanUpScheduler_createsScheduler(@Mock OutboxProperties outboxProperties,
                                                 @Mock OutboxManager manager,
                                                 @Mock DistributedLockRepository lockRepository) {
        OutboxProperties.CleanUpProperties cleanUpProperties = mock(OutboxProperties.CleanUpProperties.class);
        when(cleanUpProperties.getPolling()).thenReturn(mock(OutboxProperties.PollingProperties.class));
        when(props.getCleanUp()).thenReturn(cleanUpProperties);
        when(outboxProperties.getWorkerId()).thenReturn(UUID.randomUUID());
        when(continuableTaskDecoratorSupplier.supply(anyString())).thenReturn(mock(ContinuableTaskDecorator.class));

        try (MockedStatic<OutboxScheduleStrategyFactory> strategyFactory = mockStatic(OutboxScheduleStrategyFactory.class)) {
            strategyFactory.when(() -> OutboxScheduleStrategyFactory.create(any(), any(), any(), any()))
                    .thenReturn(mock(OutboxScheduleStrategy.class));

            OutboxScheduler scheduler = config.outboxCleanUpScheduler(
                    outboxProperties, executor, scheduleStrategyListenerSupplier, manager, lockRepository, continuableTaskDecoratorSupplier
            );

            assertThat(scheduler).isInstanceOf(OutboxCleanUpScheduler.class);
        }
    }

    @Test
    @DisplayName("UT outboxCleanUpJobCreateCommand creates DefaultOutboxJobCreateCommand")
    void outboxCleanUpJobCreateCommand_createsCommand(@Mock OutboxProperties outboxProperties,
                                                      @Mock JdbcTemplate jdbcTemplate,
                                                      @Mock Clock clock) {
        OutboxProperties.CleanUpProperties cleanUpProperties = mock(OutboxProperties.CleanUpProperties.class);
        when(cleanUpProperties.getPolling()).thenReturn(mock(OutboxProperties.PollingProperties.class));
        when(props.getCleanUp()).thenReturn(cleanUpProperties);
        when(outboxProperties.getDistributedLock()).thenReturn(mock(OutboxProperties.DistributedLockProperties.class));

        try (MockedStatic<DistributedLockPropertiesResolver> resolver = mockStatic(DistributedLockPropertiesResolver.class)) {
            DistributedLockPropertiesResolver.LockDurations lockDurations = mock(DistributedLockPropertiesResolver.LockDurations.class);
            when(lockDurations.atLeastFor()).thenReturn(Duration.ofSeconds(1).toMillis());
            when(lockDurations.atMostFor()).thenReturn(Duration.ofSeconds(5).toMillis());
            resolver.when(() -> DistributedLockPropertiesResolver.resolve(any(), any())).thenReturn(lockDurations);

            OutboxJobCreateCommand command = config.outboxCleanUpJobCreateCommand(outboxProperties, jdbcTemplate, clock);

            assertThat(command).isInstanceOf(DefaultOutboxJobCreateCommand.class);
        }
    }
}