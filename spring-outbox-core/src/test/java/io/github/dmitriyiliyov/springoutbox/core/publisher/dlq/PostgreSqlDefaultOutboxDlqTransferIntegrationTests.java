package io.github.dmitriyiliyov.springoutbox.core.publisher.dlq;

import io.github.dmitriyiliyov.springoutbox.core.it_config.BasePostgresSqlIntegrationTests;
import io.github.dmitriyiliyov.springoutbox.core.publisher.PostgreSqlOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.mockito.Mockito.mock;


@Transactional
class PostgreSqlDefaultOutboxDlqTransferIntegrationTests extends BasePostgresSqlIntegrationTests {

    private final DefaultOutboxDlqTransferVerifier verifier;

    @Autowired
    PostgreSqlDefaultOutboxDlqTransferIntegrationTests(PostgreSqlOutboxRepository outboxRepository,
                                                       PostgreSqlOutboxDlqRepository dlqRepository,
                                                       TransactionTemplate transactionTemplate,
                                                       JdbcTemplate jdbcTemplate) {
        OutboxDlqHandler handler = mock(OutboxDlqHandler.class);
        this.verifier = new DefaultOutboxDlqTransferVerifier(
                jdbcTemplate, outboxRepository, dlqRepository, transactionTemplate, handler,
                raw -> (UUID) raw
        );
    }

    @Test @DisplayName("IT transferToDlq() failed events should be moved to DLQ and deleted from outbox")
    void transferToDlq_failedEvents_movedToDlqAndDeletedFromOutbox() { verifier.transferToDlq_failedEvents_movedToDlqAndDeletedFromOutbox(); }

    @Test @DisplayName("IT transferToDlq() no failed events should do nothing")
    void transferToDlq_noFailedEvents_doesNothing() { verifier.transferToDlq_noFailedEvents_doesNothing(); }

    @Test @DisplayName("IT transferToDlq() should respect batch size")
    void transferToDlq_respectsBatchSize() { verifier.transferToDlq_respectsBatchSize(); }

    @Test @DisplayName("IT transferToDlq() should call handler with moved events")
    void transferToDlq_callsHandlerWithMovedEvents() { verifier.transferToDlq_callsHandlerWithMovedEvents(); }

    @Test @DisplayName("IT transferToDlq() empty outbox should not call handler")
    void transferToDlq_emptyOutbox_doesNotCallHandler() { verifier.transferToDlq_emptyOutbox_doesNotCallHandler(); }

    @Test @DisplayName("IT transferToDlq() handler exception should not rollback transfer")
    void transferToDlq_handlerException_doesNotRollback() { verifier.transferToDlq_handlerException_doesNotRollback(); }

    @Test @DisplayName("IT transferToDlq() should preserve event data")
    void transferToDlq_preservesEventData() { verifier.transferToDlq_preservesEventData(); }

    @Test @DisplayName("IT transferFromDlq() TO_RETRY events should be moved back to outbox")
    void transferFromDlq_toRetryEvents_movedBackToOutbox() { verifier.transferFromDlq_toRetryEvents_movedBackToOutbox(); }

    @Test @DisplayName("IT transferFromDlq() no TO_RETRY events should do nothing")
    void transferFromDlq_noToRetryEvents_doesNothing() { verifier.transferFromDlq_noToRetryEvents_doesNothing(); }

    @Test @DisplayName("IT transferFromDlq() should respect batch size")
    void transferFromDlq_respectsBatchSize() { verifier.transferFromDlq_respectsBatchSize(); }

    @Test @DisplayName("IT transferFromDlq() should reset retry count to zero")
    void transferFromDlq_resetsRetryCountToZero() { verifier.transferFromDlq_resetsRetryCountToZero(); }

    @Test @DisplayName("IT transferFromDlq() should preserve event data")
    void transferFromDlq_preservesEventData() { verifier.transferFromDlq_preservesEventData(); }
}
