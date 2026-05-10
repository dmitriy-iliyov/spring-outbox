//package io.github.dmitriyiliyov.springoutbox.core.publisher;
//
//import io.github.dmitriyiliyov.springoutbox.core.ContinuableTaskDecorator;
//import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
//import io.github.dmitriyiliyov.springoutbox.core.it.BasePostgresSqlIntegrationTests;
//import io.github.dmitriyiliyov.springoutbox.core.polling.OutboxScheduleStrategy;
//import io.github.dmitriyiliyov.springoutbox.core.utils.DefaultResultSetMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.Clock;
//import java.util.UUID;
//
//@Transactional
//class PostgreSqlOutboxPollingSchedulerIntegrationTests extends BasePostgresSqlIntegrationTests {
//
//    private OutboxPollingSchedulerVerifier verifier;
//
//    @MockBean
//    private OutboxSender outboxSender;
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
//    @Autowired
//    private Clock clock;
//
//    @BeforeEach
//    void setUp() {
//        Mockito.reset(outboxSender, scheduleStrategy);
//
//        Mockito.doAnswer(invocation -> {
//            Runnable task = invocation.getArgument(0);
//            task.run();
//            return null;
//        }).when(scheduleStrategy).scheduleExecution(Mockito.any());
//
//        OutboxPublisherPropertiesHolder.EventPropertiesHolder properties = createProperties();
//        DefaultOutboxProcessor processor = new DefaultOutboxProcessor(outboxManager, outboxSender, clock);
//        ContinuableTaskDecorator decorator = task -> task;
//
//        OutboxPollingScheduler scheduler = new OutboxPollingScheduler(
//                properties, scheduleStrategy, processor, decorator
//        );
//
//        this.verifier = new OutboxPollingSchedulerVerifier(
//                jdbcTemplate,
//                outboxRepository,
//                scheduler,
//                outboxSender,
//                raw -> (UUID) raw,
//                id -> id,
//                new DefaultResultSetMapper()
//        );
//    }
//
//    @Test
//    @DisplayName("IT schedule() should process pending events successfully")
//    void schedule_pendingEvents_processedSuccessfully() {
//        verifier.schedule_pendingEvents_processedSuccessfully();
//    }
//
//    @Test
//    @DisplayName("IT schedule() should complete without error when no events")
//    void schedule_noEvents_completesWithoutError() {
//        verifier.schedule_noEvents_completesWithoutError();
//    }
//
//    @Test
//    @DisplayName("IT schedule() should respect batch size limit")
//    void schedule_batchSizeLimit_processesCorrectAmount() {
//        verifier.schedule_batchSizeLimit_processesCorrectAmount();
//    }
//
//    @Test
//    @DisplayName("IT schedule() should retry failed events with backoff")
//    void schedule_failedEvents_retriesWithBackoff() {
//        verifier.schedule_failedEvents_retriesWithBackoff();
//    }
//
//    @Test
//    @DisplayName("IT schedule() should handle multiple batches in continuous processing")
//    void schedule_continuousProcessing_handlesMultipleBatches() {
//        verifier.schedule_continuousProcessing_handlesMultipleBatches();
//    }
//
//    private OutboxPublisherPropertiesHolder.EventPropertiesHolder createProperties() {
//        OutboxPublisherPropertiesHolder.EventPropertiesHolder props = Mockito.mock(OutboxPublisherPropertiesHolder.EventPropertiesHolder.class);
//        Mockito.when(props.getEventType()).thenReturn("ORDER_CREATED");
//        Mockito.when(props.getBatchSize()).thenReturn(10);
//        Mockito.when(props.getTopic()).thenReturn("orders.topic");
//        Mockito.when(props.getMaxRetries()).thenReturn(3);
//        Mockito.when(props.backoffMultiplier()).thenReturn(2.0);
//        Mockito.when(props.backoffDelay()).thenReturn(10L);
//        return props;
//    }
//}