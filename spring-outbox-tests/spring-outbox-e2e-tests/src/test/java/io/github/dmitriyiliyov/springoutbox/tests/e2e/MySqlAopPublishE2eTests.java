package io.github.dmitriyiliyov.springoutbox.tests.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.stream.Stream;

public class MySqlAopPublishE2eTests extends BaseMySqlIntegrationTests {

    private final AopPublishE2eVerifier verifier;

    static Stream<Arguments> arguments() {
        return Stream.of(
                Arguments.of(100),
                Arguments.of(1000)
        );
    }

    public MySqlAopPublishE2eTests(
            @Qualifier("mysqlBusinessService") BusinessService service,
            @Qualifier("outboxTransactionAwareJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        this.verifier = new AopPublishE2eVerifier(
                service,
                jdbcTemplate,
                rs -> {
                    ByteBuffer bb = ByteBuffer.wrap(rs.getBytes("verify_id"));
                    return new UUID(bb.getLong(), bb.getLong());
                }
        );
    }

    @BeforeEach
    void cleanUpBefore() {
        verifier.cleanUpQueries();
    }

    @AfterEach
    void cleanUpAfter() {
        verifier.cleanUpQueries();
    }

    @Test
    @DisplayName("E2E publish() should save single event to both business and outbox tables")
    void publish_shouldSaveEvent() {
        verifier.publish_shouldSaveEvent();
    }

    @MethodSource("arguments")
    @ParameterizedTest
    @DisplayName("E2E publish() should save multiple events to both business and outbox tables")
    void publish_shouldSaveEvents(int eventCount) {
        verifier.publish_shouldSaveEvents(eventCount);
    }

    @Test
    @DisplayName("E2E publish() should save single returned event to both business and outbox tables")
    void publish_successSaveReturnedEvent() {
        verifier.publish_successSaveReturnedEvent();
    }

    @MethodSource("arguments")
    @ParameterizedTest
    @DisplayName("E2E publish() should save multiple returned events to both business and outbox tables")
    void publish_successSaveReturnedEvents(int eventCount) {
        verifier.publish_successSaveReturnedEvents(eventCount);
    }

    @Test
    @DisplayName("E2E publish() should rollback both business and outbox when business transaction fails (single event)")
    void publishEvent_shouldThrows_whenBusinessTransactionFailed() {
        verifier.publishEvent_shouldThrows_whenBusinessTransactionFailed();
    }

    @Test
    @DisplayName("E2E publish() should rollback both business and outbox when business transaction fails (multiple events)")
    void publishEvents_shouldThrows_whenBusinessTransactionFailed() {
        verifier.publishEvents_shouldThrows_whenBusinessTransactionFailed();
    }

    @Test
    @DisplayName("E2E publish() should rollback both business and outbox when business transaction fails (single returned event)")
    void publishEventWithReturnedEvent_shouldThrows_whenBusinessTransactionFailed() {
        verifier.publishEventWithReturnedEvent_shouldThrows_whenBusinessTransactionFailed();
    }

    @Test
    @DisplayName("E2E publish() should rollback both business and outbox when business transaction fails (multiple returned events)")
    void publishEventWithReturnedEvents_shouldThrows_whenBusinessTransactionFailed() {
        verifier.publishEventsWithReturnedEvents_shouldThrows_whenBusinessTransactionFailed();
    }
}
