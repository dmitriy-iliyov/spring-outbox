package io.github.dmitriyiliyov.springoutbox.tests.integration.consume.kafka;

import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxHeaders;
import io.github.dmitriyiliyov.springoutbox.tests.integration.domain.BusinessEvent;
import io.github.dmitriyiliyov.springoutbox.tests.integration.utils.IdExtractor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.awaitility.Awaitility;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class KafkaConsumerIntegrationVerifier {

    private static final Duration AWAIT_AT_MOST = Duration.ofSeconds(15);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(200);
    private static final String EVENT_TYPE = "test-business-event";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final IdExtractor idExtractor;
    private final KafkaConsumerFaultyBusinessService faultyService;
    private final KafkaTestUtils kafkaTestUtils;

    public KafkaConsumerIntegrationVerifier(KafkaTemplate<String, Object> kafkaTemplate,
                                            JdbcTemplate jdbcTemplate,
                                            IdExtractor idExtractor,
                                            KafkaConsumerFaultyBusinessService faultyService,
                                            KafkaTestUtils kafkaTestUtils) {
        this.kafkaTemplate = kafkaTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.idExtractor = idExtractor;
        this.faultyService = faultyService;
        this.kafkaTestUtils = kafkaTestUtils;
    }

    public void consume_shouldSaveToBusinessAndConsumedTables() {
        UUID eventId  = UUID.randomUUID();
        UUID verifyId = UUID.randomUUID();

        sendSingle(eventId, verifyId);

        awaitBusinessCount(1);

        assertThat(selectBusinessVerifyIds()).containsOnly(verifyId);
        assertThat(selectConsumedEventIds()).containsOnly(eventId);
    }

    /**
     * Idempotency: same message sent twice -> business operation executed only once.
     */
    public void consume_shouldBeIdempotent_whenSameMessageReceivedTwice() {
        UUID eventId  = UUID.randomUUID();
        UUID verifyId = UUID.randomUUID();

        sendSingle(eventId, verifyId);
        awaitBusinessCount(1);

        sendSingle(eventId, verifyId);
        awaitStableBusinessCount(1);

        assertThat(selectBusinessVerifyIds()).containsOnly(verifyId);
        assertThat(selectConsumedEventIds()).containsOnly(eventId);
    }

    /**
     * Multiple distinct single messages are all saved.
     */
    public void consume_shouldSaveAllSingleMessages(int count) {
        List<UUID> eventIds  = generateIds(count);
        List<UUID> verifyIds = generateIds(count);

        for (int i = 0; i < count; i++) {
            sendSingle(eventIds.get(i), verifyIds.get(i));
        }

        awaitBusinessCount(count);

        assertThat(selectBusinessVerifyIds())
                .containsExactlyInAnyOrder(verifyIds.toArray(new UUID[0]));
        assertThat(selectConsumedEventIds())
                .containsExactlyInAnyOrder(eventIds.toArray(new UUID[0]));
    }

    public void consume_shouldSaveBatchToBusinessAndConsumedTables(int batchSize) {
        List<UUID> eventIds  = generateIds(batchSize);
        List<UUID> verifyIds = generateIds(batchSize);

        sendBatch(eventIds, verifyIds);

        awaitBusinessCount(batchSize);

        assertThat(selectBusinessVerifyIds())
                .containsExactlyInAnyOrder(verifyIds.toArray(new UUID[0]));
        assertThat(selectConsumedEventIds())
                .containsExactlyInAnyOrder(eventIds.toArray(new UUID[0]));
    }

    public void consume_shouldBeIdempotent_whenSameBatchReceivedTwice(int batchSize) {
        List<UUID> eventIds  = generateIds(batchSize);
        List<UUID> verifyIds = generateIds(batchSize);

        sendBatch(eventIds, verifyIds);
        awaitBusinessCount(batchSize);

        sendBatch(eventIds, verifyIds);
        awaitStableBusinessCount(batchSize);

        assertThat(selectBusinessVerifyIds())
                .containsExactlyInAnyOrder(verifyIds.toArray(new UUID[0]));
        assertThat(selectConsumedEventIds())
                .containsExactlyInAnyOrder(eventIds.toArray(new UUID[0]));
    }

    /**
     * Partial duplicates: a batch where half the records were already consumed.
     * Only the new half must be inserted into business_events.
     */
    public void consume_shouldProcessOnlyNewMessages_whenBatchContainsDuplicates() {
        List<UUID> firstEventIds  = generateIds(3);
        List<UUID> firstVerifyIds = generateIds(3);
        sendBatch(firstEventIds, firstVerifyIds);
        awaitBusinessCount(3);

        List<UUID> newEventIds  = generateIds(3);
        List<UUID> newVerifyIds = generateIds(3);

        List<UUID> mixedEventIds  = new ArrayList<>(firstEventIds);
        mixedEventIds.addAll(newEventIds);
        List<UUID> mixedVerifyIds = new ArrayList<>(firstVerifyIds);
        mixedVerifyIds.addAll(newVerifyIds);

        sendBatch(mixedEventIds, mixedVerifyIds);
        awaitBusinessCount(6);

        List<UUID> allVerifyIds = new ArrayList<>(firstVerifyIds);
        allVerifyIds.addAll(newVerifyIds);

        assertThat(selectBusinessVerifyIds())
                .containsExactlyInAnyOrder(allVerifyIds.toArray(new UUID[0]));

        List<UUID> allEventIds = new ArrayList<>(firstEventIds);
        allEventIds.addAll(newEventIds);

        assertThat(selectConsumedEventIds())
                .containsExactlyInAnyOrder(allEventIds.toArray(new UUID[0]));
    }

    public void consume_shouldRollbackBothTables_whenBusinessOperationFails() {
        faultyService.setShouldFail(true);

        UUID eventId  = UUID.randomUUID();
        UUID verifyId = UUID.randomUUID();

        sendToFailingTopic(eventId, verifyId);

        awaitStableBusinessCount(0);

        assertThat(selectBusinessVerifyIds()).isEmpty();
        assertThat(selectConsumedEventIds()).isEmpty();
    }

    public void consume_shouldRollbackBothTables_whenBatchOperationFails(int batchSize) {
        faultyService.setShouldFail(true);

        List<UUID> eventIds  = generateIds(batchSize);
        List<UUID> verifyIds = generateIds(batchSize);

        sendBatchToFailingTopic(eventIds, verifyIds);

        awaitStableBusinessCount(0);

        assertThat(selectBusinessVerifyIds()).isEmpty();
        assertThat(selectConsumedEventIds()).isEmpty();
    }

    public void consume_shouldBeRetryable_afterTransactionRollback() {
        faultyService.setShouldFail(true);

        UUID eventId  = UUID.randomUUID();
        UUID verifyId = UUID.randomUUID();

        sendToFailingTopic(eventId, verifyId);
        awaitStableBusinessCount(0);

        assertThat(selectBusinessVerifyIds()).isEmpty();
        assertThat(selectConsumedEventIds()).isEmpty();

        faultyService.setShouldFail(false);
        sendToFailingTopic(eventId, verifyId);

        awaitBusinessCount(1);

        assertThat(selectBusinessVerifyIds()).containsOnly(verifyId);
        assertThat(selectConsumedEventIds()).containsOnly(eventId);
    }

    public void consume_shouldBeBatchRetryable_afterTransactionRollback(int batchSize) {
        faultyService.setShouldFail(true);

        List<UUID> eventIds  = generateIds(batchSize);
        List<UUID> verifyIds = generateIds(batchSize);

        sendBatchToFailingTopic(eventIds, verifyIds);
        awaitStableBusinessCount(0);

        assertThat(selectBusinessVerifyIds()).isEmpty();
        assertThat(selectConsumedEventIds()).isEmpty();

        faultyService.setShouldFail(false);
        sendBatchToFailingTopic(eventIds, verifyIds);

        awaitBusinessCount(batchSize);

        assertThat(selectBusinessVerifyIds())
                .containsExactlyInAnyOrder(verifyIds.toArray(new UUID[0]));
        assertThat(selectConsumedEventIds())
                .containsExactlyInAnyOrder(eventIds.toArray(new UUID[0]));
    }

    private void sendSingle(UUID eventId, UUID verifyId) {
        kafkaTemplate.send(buildRecord(KafkaConsumerBusinessService.SINGLE_TOPIC, eventId, verifyId));
    }

    private void sendBatch(List<UUID> eventIds, List<UUID> verifyIds) {
        for (int i = 0; i < eventIds.size(); i++) {
            kafkaTemplate.send(buildRecord(KafkaConsumerBusinessService.BATCH_TOPIC, eventIds.get(i), verifyIds.get(i)));
        }
    }

    /**
     * Builds a {@link ProducerRecord} with the two outbox headers required by
     * {@link io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxEventIdResolveManager}.
     * The message value is the verifyId, which the listener inserts into business_events.
     */
    private ProducerRecord<String, Object> buildRecord(String topic, UUID eventId, UUID verifyId) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, BusinessEvent.of(verifyId));
        record.headers().add(new RecordHeader(
                OutboxHeaders.EVENT_ID.getValue(),
                eventId.toString().getBytes(StandardCharsets.UTF_8)
        ));
        record.headers().add(new RecordHeader(
                OutboxHeaders.EVENT_TYPE.getValue(),
                EVENT_TYPE.getBytes(StandardCharsets.UTF_8)
        ));
        return record;
    }

    private void awaitBusinessCount(int expectedCount) {
        Awaitility.await()
                .atMost(AWAIT_AT_MOST)
                .pollInterval(POLL_INTERVAL)
                .untilAsserted(() ->
                        assertThat(countBusinessEvents()).isEqualTo(expectedCount)
                );
    }

    private void awaitStableBusinessCount(int stableCount) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(POLL_INTERVAL)
                .during(Duration.ofSeconds(2))
                .untilAsserted(() ->
                        assertThat(countBusinessEvents()).isEqualTo(stableCount)
                );
    }

    private int countBusinessEvents() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM business_events", Integer.class);
        return count != null ? count : 0;
    }

    private List<UUID> selectBusinessVerifyIds() {
        return jdbcTemplate.query(
                "SELECT verify_id FROM business_events",
                (rs, n) -> idExtractor.extract("verify_id", rs)
        );
    }

    private List<UUID> selectConsumedEventIds() {
        return jdbcTemplate.query(
                "SELECT id FROM outbox_consumed_events",
                (rs, n) -> idExtractor.extract("id", rs)
        );
    }

    private static List<UUID> generateIds(int count) {
        List<UUID> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) ids.add(UUID.randomUUID());
        return ids;
    }

    private void sendToFailingTopic(UUID eventId, UUID verifyId) {
        kafkaTemplate.send(buildRecord(KafkaConsumerFaultyBusinessService.SINGLE_FAILING_TOPIC, eventId, verifyId));
    }

    private void sendBatchToFailingTopic(List<UUID> eventIds, List<UUID> verifyIds) {
        for (int i = 0; i < eventIds.size(); i++) {
            kafkaTemplate.send(buildRecord(KafkaConsumerFaultyBusinessService.BATCH_FAILING_TOPIC, eventIds.get(i), verifyIds.get(i)));
        }
    }

    public void cleanUpQueries() {
        faultyService.setShouldFail(false);
        jdbcTemplate.execute("DELETE FROM business_events");
        jdbcTemplate.execute("DELETE FROM outbox_consumed_events");
        kafkaTestUtils.resetKafkaTopics(List.of(
                KafkaConsumerBusinessService.SINGLE_TOPIC,
                KafkaConsumerBusinessService.BATCH_TOPIC,
                KafkaConsumerFaultyBusinessService.SINGLE_FAILING_TOPIC,
                KafkaConsumerFaultyBusinessService.BATCH_FAILING_TOPIC
        ));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}