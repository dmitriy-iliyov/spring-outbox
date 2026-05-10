//package io.github.dmitriyiliyov.springoutbox.core.publisher;
//
//import io.github.dmitriyiliyov.springoutbox.core.OutboxPublisherPropertiesHolder;
//import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
//import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;
//import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.SenderResult;
//import io.github.dmitriyiliyov.springoutbox.core.utils.ResultSetMapper;
//import org.awaitility.Awaitility;
//import org.springframework.jdbc.core.JdbcTemplate;
//
//import java.time.Instant;
//import java.time.temporal.ChronoUnit;
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//import java.util.stream.Collectors;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.when;
//
//class OutboxPollingSchedulerVerifier {
//
//    private final JdbcTemplate jdbcTemplate;
//    private final OutboxRepository outboxRepository;
//    private final OutboxPollingScheduler scheduler;
//    private final OutboxSender outboxSenderMock;
//    private final IdExtractor idExtractor;
//    private final IdPreparer idPreparer;
//    private final ResultSetMapper mapper;
//
//    @FunctionalInterface
//    interface IdExtractor {
//        UUID extract(Object raw);
//    }
//
//    @FunctionalInterface
//    interface IdPreparer {
//        Object prepare(UUID id);
//    }
//
//    OutboxPollingSchedulerVerifier(
//            JdbcTemplate jdbcTemplate,
//            OutboxRepository outboxRepository,
//            OutboxPollingScheduler scheduler,
//            OutboxSender outboxSenderMock,
//            IdExtractor idExtractor,
//            IdPreparer idPreparer,
//            ResultSetMapper mapper
//    ) {
//        this.jdbcTemplate = jdbcTemplate;
//        this.outboxRepository = outboxRepository;
//        this.scheduler = scheduler;
//        this.outboxSenderMock = outboxSenderMock;
//        this.idExtractor = idExtractor;
//        this.idPreparer = idPreparer;
//        this.mapper = mapper;
//    }
//
//    void schedule_pendingEvents_processedSuccessfully() {
//        OutboxEvent event1 = saveOutboxEvent(EventStatus.PENDING, 0);
//        OutboxEvent event2 = saveOutboxEvent(EventStatus.PENDING, 0);
//
//        when(outboxSenderMock.sendEvents(any(), any())).thenAnswer(invocation -> {
//            List<OutboxEvent> events = invocation.getArgument(1);
//            Set<UUID> allIds = events.stream().map(OutboxEvent::getId).collect(Collectors.toSet());
//            return new SenderResult(allIds, Collections.emptySet());
//        });
//
//        scheduler.schedule();
//
//        Awaitility.await()
//                .atMost(5, TimeUnit.SECONDS)
//                .untilAsserted(() -> {
//                    List<UUID> processedIds = queryIdsByStatus("PROCESSED");
//                    assertThat(processedIds).containsExactlyInAnyOrder(event1.getId(), event2.getId());
//                });
//    }
//
//    void schedule_noEvents_completesWithoutError() {
//        scheduler.schedule();
//
//        Awaitility.await()
//                .pollDelay(100, TimeUnit.MILLISECONDS)
//                .atMost(2, TimeUnit.SECONDS)
//                .untilAsserted(() -> {
//                    List<UUID> allIds = queryAllEventIds();
//                    assertThat(allIds).isEmpty();
//                });
//    }
//
//    void schedule_batchSizeLimit_processesCorrectAmount() {
//        for (int i = 0; i < 15; i++) {
//            saveOutboxEvent(EventStatus.PENDING, 0);
//        }
//
//        when(outboxSenderMock.sendEvents(any(), any())).thenAnswer(invocation -> {
//            List<OutboxEvent> events = invocation.getArgument(1);
//            Set<UUID> allIds = events.stream().map(OutboxEvent::getId).collect(Collectors.toSet());
//            return new SenderResult(allIds, Collections.emptySet());
//        });
//
//        scheduler.schedule();
//
//        Awaitility.await()
//                .atMost(5, TimeUnit.SECONDS)
//                .untilAsserted(() -> {
//                    List<UUID> processedIds = queryIdsByStatus("PROCESSED");
//                    assertThat(processedIds).hasSizeLessThanOrEqualTo(10);
//                });
//    }
//
//    void schedule_failedEvents_retriesWithBackoff() {
//        OutboxEvent event = saveOutboxEvent(EventStatus.PENDING, 1);
//
//        when(outboxSenderMock.sendEvents(any(), any())).thenReturn(
//                new SenderResult(Collections.emptySet(), Set.of(event.getId()))
//        );
//
//        scheduler.schedule();
//
//        Awaitility.await()
//                .atMost(5, TimeUnit.SECONDS)
//                .untilAsserted(() -> {
//                    OutboxEvent updated = getEvent(event.getId());
//                    assertThat(updated.getStatus()).isEqualTo(EventStatus.PENDING);
//                    assertThat(updated.getRetryCount()).isEqualTo(2);
//                    assertThat(updated.getNextRetryAt()).isAfter(Instant.now());
//                });
//    }
//
//    void schedule_continuousProcessing_handlesMultipleBatches() {
//        for (int i = 0; i < 25; i++) {
//            saveOutboxEvent(EventStatus.PENDING, 0);
//        }
//
//        when(outboxSenderMock.sendEvents(any(), any())).thenAnswer(invocation -> {
//            List<OutboxEvent> events = invocation.getArgument(1);
//            Set<UUID> allIds = events.stream().map(OutboxEvent::getId).collect(Collectors.toSet());
//            return new SenderResult(allIds, Collections.emptySet());
//        });
//
//        scheduler.schedule();
//
//        Awaitility.await()
//                .atMost(10, TimeUnit.SECONDS)
//                .untilAsserted(() -> {
//                    List<UUID> processedIds = queryIdsByStatus("PROCESSED");
//                    assertThat(processedIds).hasSizeGreaterThanOrEqualTo(10);
//                });
//    }
//
//    private List<UUID> queryIdsByStatus(String status) {
//        return jdbcTemplate.queryForList(
//                "SELECT id FROM outbox_events WHERE status = ?", status
//        ).stream().map(row -> idExtractor.extract(row.get("id"))).toList();
//    }
//
//    private List<UUID> queryAllEventIds() {
//        return jdbcTemplate.queryForList("SELECT id FROM outbox_events")
//                .stream().map(row -> idExtractor.extract(row.get("id"))).toList();
//    }
//
//    private OutboxEvent getEvent(UUID id) {
//        return jdbcTemplate.query(
//                "SELECT * FROM outbox_events WHERE id = ?",
//                ps -> ps.setObject(1, idPreparer.prepare(id)),
//                (rs, n) -> mapper.toEvent(rs)
//        ).getFirst();
//    }
//
//    private OutboxEvent saveOutboxEvent(EventStatus status, int retryCount) {
//        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
//        Instant nextRetryTime = now.minusSeconds(60);
//
//        OutboxEvent event = new OutboxEvent(
//                UUID.randomUUID(), status, "ORDER_CREATED",
//                "io.example.OrderCreated", "{\"orderId\":\"123\"}",
//                retryCount, nextRetryTime, now, now
//        );
//        outboxRepository.saveBatch(List.of(event));
//        return event;
//    }
//}