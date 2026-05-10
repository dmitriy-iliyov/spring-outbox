//package io.github.dmitriyiliyov.springoutbox.core.publisher;
//
//import io.github.dmitriyiliyov.springoutbox.core.ContinuableTaskDecorator;
//import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
//import io.github.dmitriyiliyov.springoutbox.core.it.BasePostgresSqlIntegrationTests;
//import io.github.dmitriyiliyov.springoutbox.core.polling.OutboxScheduleStrategy;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.Duration;
//import java.util.UUID;
//
//@Transactional
//class PostgreSqlOutboxRecoverySchedulerIntegrationTests extends BasePostgresSqlIntegrationTests {
//
//    private OutboxRecoverySchedulerVerifier verifier;
//
//    @MockBean
//    private OutboxScheduleStrategy scheduleStrategy;
//
//    @Autowired
//    private OutboxManager outboxManager;
//
//    @Autowired
//    private PostgreSqlOutboxRepository outboxRepository;
//
//    @Autowired
//    private JdbcTemplate jdbcTemplate;
//
//    @BeforeEach
//    void setUp() {
//        Mockito.reset(scheduleStrategy);
//
//        Mockito.doAnswer(invocation -> {
//            Runnable task = invocation.getArgument(0);
//            task.run();
//            return null;
//        }).when(scheduleStrategy).scheduleExecution(Mockito.any());
//
//        OutboxPublisherPropertiesHolder.StuckRecoveryPropertiesHolder properties = createProperties();
//        ContinuableTaskDecorator decorator = task -> task;
//
//        OutboxRecoveryScheduler scheduler = new OutboxRecoveryScheduler(
//                properties, scheduleStrategy, outboxManager, decorator
//        );
//
//        this.verifier = new OutboxRecoverySchedulerVerifier(
//                jdbcTemplate,
//                outboxRepository,
//                scheduler,
//                raw -> (UUID) raw
//        );
//    }
//
//    @Test
//    @DisplayName("IT schedule() should recover stuck events successfully")
//    void schedule_stuckEvents_recoveredSuccessfully() {
//        verifier.schedule_stuckEvents_recoveredSuccessfully();
//    }
//
//    @Test
//    @DisplayName("IT schedule() should complete without error when no stuck events")
//    void schedule_noStuckEvents_completesWithoutError() {
//        verifier.schedule_noStuckEvents_completesWithoutError();
//    }
//
//    @Test
//    @DisplayName("IT schedule() should respect batch size limit")
//    void schedule_batchSizeLimit_recoversCorrectAmount() {
//        verifier.schedule_batchSizeLimit_recoversCorrectAmount();
//    }
//
//    @Test
//    @DisplayName("IT schedule() should handle multiple batches in continuous recovery")
//    void schedule_continuousRecovery_handlesMultipleBatches() {
//        verifier.schedule_continuousRecovery_handlesMultipleBatches();
//    }
//
//    @Test
//    @DisplayName("IT schedule() should recover only stuck events")
//    void schedule_mixedEvents_recoversOnlyStuck() {
//        verifier.schedule_mixedEvents_recoversOnlyStuck();
//    }
//
//    private OutboxPublisherPropertiesHolder.StuckRecoveryPropertiesHolder createProperties() {
//        OutboxPublisherPropertiesHolder.StuckRecoveryPropertiesHolder props = Mockito.mock(OutboxPublisherPropertiesHolder.StuckRecoveryPropertiesHolder.class);
//        Mockito.when(props.getMaxBatchProcessingTime()).thenReturn(Duration.ofMillis(60));
//        Mockito.when(props.getBatchSize()).thenReturn(10);
//        return props;
//    }
//}