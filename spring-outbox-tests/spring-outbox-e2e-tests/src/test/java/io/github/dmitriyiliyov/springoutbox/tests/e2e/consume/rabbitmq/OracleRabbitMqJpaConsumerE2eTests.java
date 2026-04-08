package io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.rabbitmq;

import io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.test_template.BaseOracleE2eTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.stream.Stream;

public class OracleRabbitMqJpaConsumerE2eTests extends BaseOracleE2eTests {

    private final RabbitMqConsumerE2eVerifier verifier;

    static Stream<Arguments> batchSizeArguments() {
        return Stream.of(
                Arguments.of(100)
        );
    }

    public OracleRabbitMqJpaConsumerE2eTests(
            RabbitTemplate testRabbitTemplate,
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("rabbitMqJpaFaultyConsumerBusinessService") RabbitMqConsumerFaultyBusinessService rabbitMqFaultyJpaConsumerBusinessService
    ) {
        this.verifier = new RabbitMqConsumerE2eVerifier(
                testRabbitTemplate,
                jdbcTemplate,
                rs -> {
                    ByteBuffer bb = ByteBuffer.wrap(rs.getBytes("verify_id"));
                    return new UUID(bb.getLong(), bb.getLong());
                },
                rs -> {
                    ByteBuffer bb = ByteBuffer.wrap(rs.getBytes("id"));
                    return new UUID(bb.getLong(), bb.getLong());
                },
                rabbitMqFaultyJpaConsumerBusinessService
        );
    }

    @BeforeEach
    void cleanUpBefore() { verifier.cleanUpQueries(); }

    @Test
    @DisplayName("E2E consume() should save event id to consumed table and run business operation")
    void consume_shouldSaveToBusinessAndConsumedTables() {
        verifier.consume_shouldSaveToBusinessAndConsumedTables();
    }

    @Test
    @DisplayName("E2E consume() should be idempotent: same message received twice -> business op runs once")
    void consume_shouldBeIdempotent_whenSameMessageReceivedTwice() {
        verifier.consume_shouldBeIdempotent_whenSameMessageReceivedTwice();
    }

    @MethodSource("batchSizeArguments")
    @ParameterizedTest
    @DisplayName("E2E consume() should process all distinct single messages")
    void consume_shouldSaveAllSingleMessages(int count) {
        verifier.consume_shouldSaveAllSingleMessages(count);
    }

    @MethodSource("batchSizeArguments")
    @ParameterizedTest
    @DisplayName("E2E consume() batch: all new records saved to both tables")
    void consume_shouldSaveBatchToBusinessAndConsumedTables(int batchSize) {
        verifier.consume_shouldSaveBatchToBusinessAndConsumedTables(batchSize);
    }

    @MethodSource("batchSizeArguments")
    @ParameterizedTest
    @DisplayName("E2E consume() batch: same batch delivered twice -> each record processed once")
    void consume_shouldBeIdempotent_whenSameBatchReceivedTwice(int batchSize) {
        verifier.consume_shouldBeIdempotent_whenSameBatchReceivedTwice(batchSize);
    }

    @Test
    @DisplayName("E2E consume() batch: only new records processed when batch contains duplicates")
    void consume_shouldProcessOnlyNewMessages_whenBatchContainsDuplicates() {
        verifier.consume_shouldProcessOnlyNewMessages_whenBatchContainsDuplicates();
    }

    @Test
    @DisplayName("E2E consume() should rollback both tables when business operation throws (single)")
    void consume_shouldRollbackBothTables_whenBusinessOperationFails() {
        verifier.consume_shouldRollbackBothTables_whenBusinessOperationFails();
    }

    @MethodSource("batchSizeArguments")
    @ParameterizedTest
    @DisplayName("E2E consume() should rollback both tables when batch operation throws")
    void consume_shouldRollbackBothTables_whenBatchOperationFails(int batchSize) {
        verifier.consume_shouldRollbackBothTables_whenBatchOperationFails(batchSize);
    }

    @Disabled
    @Test
    @DisplayName("E2E consume() should allow retry after rollback: event id not burned on failure (single)")
    void consume_shouldBeRetryable_afterTransactionRollback() {
        verifier.consume_shouldBeRetryable_afterTransactionRollback();
    }

    @Disabled
    @MethodSource("batchSizeArguments")
    @ParameterizedTest
    @DisplayName("E2E consume() should allow retry after rollback: event id not burned on failure (batch)")
    void consume_shouldBeBatchRetryable_afterTransactionRollback(int batchSize) {
        verifier.consume_shouldBeBatchRetryable_afterTransactionRollback(batchSize);
    }
}
