//package io.github.dmitriyiliyov.springoutbox.core.publisher;
//
//import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.EventStatus;
//import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxEvent;
//import org.awaitility.Awaitility;
//import org.springframework.jdbc.core.JdbcTemplate;
//
//import java.time.Instant;
//import java.time.temporal.ChronoUnit;
//import java.util.List;
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//class OutboxRecoverySchedulerVerifier {
//
//    private final JdbcTemplate jdbcTemplate;
//    private final OutboxRepository outboxRepository;
//    private final OutboxRecoveryScheduler scheduler;
//    private final IdExtractor idExtractor;
//
//    @FunctionalInterface
//    interface IdExtractor {
//        UUID extract(Object raw);
//    }
//
//    OutboxRecoverySchedulerVerifier(
//            JdbcTemplate jdbcTemplate,
//            OutboxRepository outboxRepository,
//            OutboxRecoveryScheduler scheduler,
//            IdExtractor idExtractor
//    ) {
//        this.jdbcTemplate = jdbcTemplate;
//        this.outboxRepository = outboxRepository;
//        this.scheduler = scheduler;
//        this.idExtractor = idExtractor;
//    }
//
//    void schedule_stuckEvents_recoveredSuccessfully() {
//        OutboxEvent stuckEvent = saveStuckEvent();
//        OutboxEvent normalEvent = saveNormalEvent();
//
//        scheduler.schedule();
//
//        Awaitility.await()
//                .atMost(5, TimeUnit.SECONDS)
//                .untilAsserted(() -> {
//                    List<UUID> pendingIds = queryIdsByStatus("PENDING");
//                    assertThat(pendingIds).contains(stuckEvent.getId());
//                    assertThat(pendingIds).contains(normalEvent.getId());
//                });
//    }
//
//    void schedule_noStuckEvents_completesWithoutError() {
//        OutboxEvent normalEvent = saveNormalEvent();
//
//        scheduler.schedule();
//
//        Awaitility.await()
//                .pollDelay(100, TimeUnit.MILLISECONDS)
//                .atMost(2, TimeUnit.SECONDS)
//                .untilAsserted(() -> {
//                    List<UUID> pendingIds = queryIdsByStatus("PENDING");
//                    assertThat(pendingIds).contains(normalEvent.getId());
//                });
//    }
//
//    void schedule_batchSizeLimit_recoversCorrectAmount() {
//        for (int i = 0; i < 15; i++) {
//            saveStuckEvent();
//        }
//
//        scheduler.schedule();
//
//        Awaitility.await()
//                .atMost(5, TimeUnit.SECONDS)
//                .untilAsserted(() -> {
//                    List<UUID> processingIds = queryIdsByStatus("PROCESSING");
//                    assertThat(processingIds).hasSizeLessThanOrEqualTo(5);
//                });
//    }
//
//    void schedule_continuousRecovery_handlesMultipleBatches() {
//        for (int i = 0; i < 20; i++) {
//            saveStuckEvent();
//        }
//
//        scheduler.schedule();
//
//        Awaitility.await()
//                .atMost(10, TimeUnit.SECONDS)
//                .untilAsserted(() -> {
//                    List<UUID> processingIds = queryIdsByStatus("PROCESSING");
//                    assertThat(processingIds).hasSizeGreaterThan(0);
//                });
//    }
//
//    void schedule_mixedEvents_recoversOnlyStuck() {
//        OutboxEvent stuckEvent = saveStuckEvent();
//        OutboxEvent processedEvent = saveProcessedEvent();
//
//        scheduler.schedule();
//
//        Awaitility.await()
//                .atMost(5, TimeUnit.SECONDS)
//                .untilAsserted(() -> {
//                    List<UUID> processingIds = queryIdsByStatus("PROCESSING");
//                    List<UUID> processedIds = queryIdsByStatus("PROCESSED");
//                    assertThat(processingIds).contains(stuckEvent.getId());
//                    assertThat(processedIds).contains(processedEvent.getId());
//                });
//    }
//
//    private List<UUID> queryIdsByStatus(String status) {
//        return jdbcTemplate.queryForList(
//                "SELECT id FROM outbox_events WHERE status = ?", status
//        ).stream().map(row -> idExtractor.extract(row.get("id"))).toList();
//    }
//
//    private OutboxEvent saveStuckEvent() {
//        Instant longAgo = Instant.now().minus(2, ChronoUnit.HOURS);
//        OutboxEvent event = new OutboxEvent(
//                UUID.randomUUID(), EventStatus.PROCESSED, "ORDER_CREATED",
//                "io.example.OrderCreated", "{\"orderId\":\"123\"}",
//                0, Instant.now(), longAgo, longAgo
//        );
//        outboxRepository.saveBatch(List.of(event));
//        return event;
//    }
//
//    private OutboxEvent saveNormalEvent() {
//        Instant now = Instant.now();
//        OutboxEvent event = new OutboxEvent(
//                UUID.randomUUID(), EventStatus.PENDING, "ORDER_CREATED",
//                "io.example.OrderCreated", "{\"orderId\":\"123\"}",
//                0, now, now, now
//        );
//        outboxRepository.saveBatch(List.of(event));
//        return event;
//    }
//
//    private OutboxEvent saveProcessedEvent() {
//        Instant now = Instant.now();
//        OutboxEvent event = new OutboxEvent(
//                UUID.randomUUID(), EventStatus.PROCESSED, "ORDER_CREATED",
//                "io.example.OrderCreated", "{\"orderId\":\"123\"}",
//                0, now, now, now
//        );
//        outboxRepository.saveBatch(List.of(event));
//        return event;
//    }
//}