package io.github.dmitriyiliyov.springoutbox.core.dlq;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface OutboxDlqRepository {
    void saveBatch(List<OutboxDlqEvent> dlqEvents);

    List<OutboxDlqEvent> findBatchByStatus(DlqStatus status, int batchNumber, int batchSize);

    @Transactional
    void updateStatus(UUID id, DlqStatus status);

    void updateBatchStatus(Set<UUID> ids, DlqStatus status);

    long count();

    long countByStatus(DlqStatus status);

    long countByEventTypeAndStatus(String eventType, DlqStatus status);
}
