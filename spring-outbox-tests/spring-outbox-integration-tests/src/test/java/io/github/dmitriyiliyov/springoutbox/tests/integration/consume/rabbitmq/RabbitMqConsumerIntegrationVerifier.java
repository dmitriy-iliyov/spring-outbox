package io.github.dmitriyiliyov.springoutbox.tests.integration.consume.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.springoutbox.core.publisher.domain.OutboxHeaders;
import io.github.dmitriyiliyov.springoutbox.tests.integration.domain.BusinessEvent;
import io.github.dmitriyiliyov.springoutbox.tests.integration.utils.IdExtractor;
import org.awaitility.Awaitility;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class RabbitMqConsumerIntegrationVerifier {

    private static final Duration AWAIT_AT_MOST = Duration.ofSeconds(15);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(200);
    private static final String EVENT_TYPE = "test-business-event";

    private final RabbitTemplate rabbitTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final IdExtractor idExtractor;
    private final RabbitMqConsumerFaultyBusinessService faultyService;
    private final ObjectMapper objectMapper;

    public RabbitMqConsumerIntegrationVerifier(RabbitTemplate rabbitTemplate,
                                               JdbcTemplate jdbcTemplate,
                                               IdExtractor idExtractor,
                                               RabbitMqConsumerFaultyBusinessService faultyService) {
        this.rabbitTemplate = rabbitTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.idExtractor = idExtractor;
        this.faultyService = faultyService;
        this.objectMapper = new ObjectMapper();
    }

    public void consume_shouldSaveToBusinessAndConsumedTables() {
        UUID eventId  = UUID.randomUUID();
        UUID verifyId = UUID.randomUUID();

        sendSingle(eventId, verifyId);

        awaitBusinessCount(1);

        assertThat(selectBusinessVerifyIds()).containsOnly(verifyId);
        assertThat(selectConsumedEventIds()).containsOnly(eventId);
    }

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

        sendToFailingQueue(eventId, verifyId);

        awaitStableBusinessCount(0);

        assertThat(selectBusinessVerifyIds()).isEmpty();
        assertThat(selectConsumedEventIds()).isEmpty();
    }

    public void consume_shouldRollbackBothTables_whenBatchOperationFails(int batchSize) {
        faultyService.setShouldFail(true);

        List<UUID> eventIds  = generateIds(batchSize);
        List<UUID> verifyIds = generateIds(batchSize);

        sendBatchToFailingQueue(eventIds, verifyIds);

        awaitStableBusinessCount(0);

        assertThat(selectBusinessVerifyIds()).isEmpty();
        assertThat(selectConsumedEventIds()).isEmpty();
    }

    public void consume_shouldBeRetryable_afterTransactionRollback() {
        faultyService.setShouldFail(true);

        UUID eventId  = UUID.randomUUID();
        UUID verifyId = UUID.randomUUID();

        sendToFailingQueue(eventId, verifyId);
        awaitStableBusinessCount(0);

        assertThat(selectBusinessVerifyIds()).isEmpty();
        assertThat(selectConsumedEventIds()).isEmpty();

        faultyService.setShouldFail(false);
        sendToFailingQueue(eventId, verifyId);

        awaitBusinessCount(1);

        assertThat(selectBusinessVerifyIds()).containsOnly(verifyId);
        assertThat(selectConsumedEventIds()).containsOnly(eventId);
    }

    public void consume_shouldBeBatchRetryable_afterTransactionRollback(int batchSize) {
        faultyService.setShouldFail(true);

        List<UUID> eventIds  = generateIds(batchSize);
        List<UUID> verifyIds = generateIds(batchSize);

        sendBatchToFailingQueue(eventIds, verifyIds);
        awaitStableBusinessCount(0);

        assertThat(selectBusinessVerifyIds()).isEmpty();
        assertThat(selectConsumedEventIds()).isEmpty();

        faultyService.setShouldFail(false);
        sendBatchToFailingQueue(eventIds, verifyIds);

        awaitBusinessCount(batchSize);

        assertThat(selectBusinessVerifyIds())
                .containsExactlyInAnyOrder(verifyIds.toArray(new UUID[0]));
        assertThat(selectConsumedEventIds())
                .containsExactlyInAnyOrder(eventIds.toArray(new UUID[0]));
    }

    private void sendSingle(UUID eventId, UUID verifyId) {
        rabbitTemplate.send(RabbitMqConsumerBusinessService.SINGLE_QUEUE, buildMessage(eventId, verifyId));
    }

    private void sendBatch(List<UUID> eventIds, List<UUID> verifyIds) {
        for (int i = 0; i < eventIds.size(); i++) {
            rabbitTemplate.send(RabbitMqConsumerBusinessService.BATCH_QUEUE, buildMessage(eventIds.get(i), verifyIds.get(i)));
        }
    }

    private void sendToFailingQueue(UUID eventId, UUID verifyId) {
        rabbitTemplate.send(RabbitMqConsumerFaultyBusinessService.SINGLE_FAILING_QUEUE, buildMessage(eventId, verifyId));
    }

    private void sendBatchToFailingQueue(List<UUID> eventIds, List<UUID> verifyIds) {
        for (int i = 0; i < eventIds.size(); i++) {
            rabbitTemplate.send(RabbitMqConsumerFaultyBusinessService.BATCH_FAILING_QUEUE, buildMessage(eventIds.get(i), verifyIds.get(i)));
        }
    }

    /**
     * Builds an AMQP {@link Message} whose body is a JSON-serialised {@link BusinessEvent}
     * and whose headers carry the two outbox identifiers required by
     * {@link io.github.dmitriyiliyov.springoutbox.core.consumer.OutboxEventIdResolveManager}.
     */
    private Message buildMessage(UUID eventId, UUID verifyId) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(BusinessEvent.of(verifyId));
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setHeader(OutboxHeaders.EVENT_ID.getValue(), eventId.toString());
            props.setHeader(OutboxHeaders.EVENT_TYPE.getValue(), EVENT_TYPE);
            return new Message(body, props);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize BusinessEvent", e);
        }
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
                .atMost(Duration.ofSeconds(5))
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

    public void cleanUpQueries() {
        jdbcTemplate.execute("DELETE FROM business_events");
        jdbcTemplate.execute("DELETE FROM outbox_consumed_events");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
