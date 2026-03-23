package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.it_config.BaseMySqlIntegrationTests;
import io.github.dmitriyiliyov.springoutbox.core.publisher.MySqlOutboxRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;

public class MySqlDefaultOutboxDlqTransferConcurrentIntegrationTests extends BaseMySqlIntegrationTests {

    private final DefaultOutboxDlqTransferVerifier verifier;

    @Autowired
    MySqlDefaultOutboxDlqTransferConcurrentIntegrationTests(
            MySqlOutboxRepository outboxRepository,
            MySqlOutboxDlqRepository dlqRepository,
            TransactionTemplate transactionTemplate,
            JdbcTemplate jdbcTemplate
    ) {
        OutboxDlqHandler handler = mock(OutboxDlqHandler.class);
        this.verifier = new DefaultOutboxDlqTransferVerifier(
                jdbcTemplate, outboxRepository, dlqRepository,
                transactionTemplate, handler,
                raw -> {
                    byte[] bytes = (byte[]) raw;
                    ByteBuffer bb = ByteBuffer.wrap(bytes);
                    return new UUID(bb.getLong(), bb.getLong());
                }
        );
    }

    static Stream<Arguments> concurrentArgs() {
        return Stream.of(
                Arguments.of(50, 2),
                Arguments.of(50, 1)
        );
    }

    @AfterEach
    void cleanup() {
        verifier.getJdbcTemplate().execute("DELETE FROM outbox_events");
        verifier.getJdbcTemplate().execute("DELETE FROM outbox_dlq_events");
    }

    @MethodSource("concurrentArgs")
    @ParameterizedTest
    @DisplayName("IT transferToDlq() concurrent execution should move each event exactly once")
    void transferToDlq_concurrent(int eventCount, int threadCount) throws InterruptedException {
        verifier.transferToDlq_concurrent(eventCount, threadCount);
    }

    @MethodSource("concurrentArgs")
    @ParameterizedTest
    @DisplayName("IT transferFromDlq() concurrent execution should move each event exactly once")
    void transferFromDlq_concurrent(int eventCount, int threadCount) throws InterruptedException {
        verifier.transferFromDlq_concurrent(eventCount, threadCount);
    }
}
